package com.chat.model;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Système de sauvegarde pour les messages
 * Garde en mémoire tout ce qui s'est dit dans les conversations
 */
public class MessageStorage {
  private static final String MESSAGES_DIR = "data/messages_history";
  private static final String GENERAL_CHAT_FILE = "general_chat.txt";
  private static final String PRIVATE_CHAT_PREFIX = "private_";
  private static final String GROUP_CHAT_PREFIX = "group_";

  static {
    // On crée le dossier s'il n'existe pas encore
    try {
      Files.createDirectories(Paths.get(MESSAGES_DIR));
    } catch (IOException e) {
      System.err.println("Problème pour créer le dossier des messages : " + e.getMessage());
    }
  }

  /**
   * Sauvegarde un message dans le chat général
   */
  public static void sauvegarderMessageGeneral(String expediteur, String message) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    String ligne = timestamp + "|" + expediteur + "|" + message;

    try {
      Files.write(
          Paths.get(MESSAGES_DIR, GENERAL_CHAT_FILE),
          (ligne + "\n").getBytes(),
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    } catch (IOException e) {
      System.err.println("Problème pour sauvegarder le message général : " + e.getMessage());
    }
  }

  /**
   * Sauvegarde un message privé entre deux personnes
   */
  public static void sauvegarderMessagePrive(String expediteur, String destinataire, String message) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    String ligne = timestamp + "|" + expediteur + "|" + destinataire + "|" + message;

    // Je trie les noms pour avoir toujours le même nom de fichier peu importe qui
    // écrit à qui
    String[] users = { expediteur, destinataire };
    Arrays.sort(users);
    String filename = PRIVATE_CHAT_PREFIX + users[0] + "_" + users[1] + ".txt";

    try {
      Files.write(
          Paths.get(MESSAGES_DIR, filename),
          (ligne + "\n").getBytes(),
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    } catch (IOException e) {
      System.err.println("Problème pour sauvegarder le message privé : " + e.getMessage());
    }
  }

  /**
   * Sauvegarde un message de groupe
   */
  public static void sauvegarderMessageGroupe(String nomGroupe, String expediteur, String message) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    String ligne = timestamp + "|" + expediteur + "|" + message;

    String filename = GROUP_CHAT_PREFIX + nomGroupe + ".txt";

    try {
      Files.write(
          Paths.get(MESSAGES_DIR, filename),
          (ligne + "\n").getBytes(),
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    } catch (IOException e) {
      System.err.println("Problème pour sauvegarder le message de groupe : " + e.getMessage());
    }
  }

  /**
   * Récupère tout ce qui s'est dit dans le chat général
   */
  public static List<String> chargerHistoriqueGeneral() {
    List<String> historique = new ArrayList<>();
    Path fichier = Paths.get(MESSAGES_DIR, GENERAL_CHAT_FILE);

    try {
      if (Files.exists(fichier)) {
        for (String ligne : Files.readAllLines(fichier)) {
          if (!ligne.trim().isEmpty()) {
            String[] parties = ligne.split("\\|", 3);
            if (parties.length >= 3) {
              String timestamp = parties[0];
              String expediteur = parties[1];
              String message = parties[2];

              // Je formate juste l'heure pour l'affichage
              String timeDisplay = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                  .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
              historique.add("[" + timeDisplay + "] " + expediteur + " : " + message);
            }
          }
        }
      }
    } catch (IOException e) {
      System.err.println("Problème pour charger l'historique général : " + e.getMessage());
    }

    return historique;
  }

  /**
   * Récupère l'historique d'une conversation privée
   */
  public static List<String> chargerHistoriquePrive(String utilisateur1, String utilisateur2) {
    List<String> historique = new ArrayList<>();

    // Même logique que pour la sauvegarde : je trie les noms
    String[] users = { utilisateur1, utilisateur2 };
    Arrays.sort(users);
    String filename = PRIVATE_CHAT_PREFIX + users[0] + "_" + users[1] + ".txt";

    Path fichier = Paths.get(MESSAGES_DIR, filename);

    try {
      if (Files.exists(fichier)) {
        for (String ligne : Files.readAllLines(fichier)) {
          if (!ligne.trim().isEmpty()) {
            String[] parties = ligne.split("\\|", 4);
            if (parties.length >= 4) {
              String timestamp = parties[0];
              String expediteur = parties[1];
              String destinataire = parties[2];
              String message = parties[3];

              // Même format que pour le chat général
              String timeDisplay = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                  .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
              historique.add("[" + timeDisplay + "] " + expediteur + " → " + destinataire + " : " + message);
            }
          }
        }
      }
    } catch (IOException e) {
      System.err.println("Problème pour charger l'historique privé : " + e.getMessage());
    }

    return historique;
  }

  /**
   * Récupère l'historique d'un groupe
   */
  public static List<String> chargerHistoriqueGroupe(String nomGroupe) {
    List<String> historique = new ArrayList<>();
    String filename = GROUP_CHAT_PREFIX + nomGroupe + ".txt";
    Path fichier = Paths.get(MESSAGES_DIR, filename);

    try {
      if (Files.exists(fichier)) {
        for (String ligne : Files.readAllLines(fichier)) {
          if (!ligne.trim().isEmpty()) {
            String[] parties = ligne.split("\\|", 3);
            if (parties.length >= 3) {
              String timestamp = parties[0];
              String expediteur = parties[1];
              String message = parties[2];

              // J'ajoute le nom du groupe dans l'affichage
              String timeDisplay = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                  .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
              historique.add("[" + timeDisplay + "] [" + nomGroupe + "] " + expediteur + " : " + message);
            }
          }
        }
      }
    } catch (IOException e) {
      System.err.println("Problème pour charger l'historique du groupe : " + e.getMessage());
    }

    return historique;
  }

  /**
   * Trouve toutes les conversations privées d'un utilisateur
   */
  public static List<String> obtenirConversationsPrivees(String utilisateur) {
    List<String> conversations = new ArrayList<>();

    try {
      Files.list(Paths.get(MESSAGES_DIR))
          .filter(path -> path.getFileName().toString().startsWith(PRIVATE_CHAT_PREFIX))
          .forEach(path -> {
            String filename = path.getFileName().toString();
            String[] users = filename.replace(PRIVATE_CHAT_PREFIX, "").replace(".txt", "").split("_");
            if (users.length == 2) {
              if (users[0].equals(utilisateur)) {
                conversations.add(users[1]);
              } else if (users[1].equals(utilisateur)) {
                conversations.add(users[0]);
              }
            }
          });
    } catch (IOException e) {
      System.err.println("Problème pour lire les conversations : " + e.getMessage());
    }

    return conversations;
  }

  /**
   * Trouve tous les groupes qui ont des messages sauvegardés
   */
  public static List<String> obtenirGroupesAvecHistorique() {
    List<String> groupes = new ArrayList<>();

    try {
      Files.list(Paths.get(MESSAGES_DIR))
          .filter(path -> path.getFileName().toString().startsWith(GROUP_CHAT_PREFIX))
          .forEach(path -> {
            String filename = path.getFileName().toString();
            String nomGroupe = filename.replace(GROUP_CHAT_PREFIX, "").replace(".txt", "");
            groupes.add(nomGroupe);
          });
    } catch (IOException e) {
      System.err.println("Problème pour lire les groupes : " + e.getMessage());
    }

    return groupes;
  }

  /**
   * Fait le ménage dans les anciens messages (plus de X jours)
   */
  public static void nettoyerAncienMessage(int joursAConserver) {
    LocalDateTime limite = LocalDateTime.now().minusDays(joursAConserver);

    try {
      Files.list(Paths.get(MESSAGES_DIR))
          .filter(path -> path.toString().endsWith(".txt"))
          .forEach(path -> {
            try {
              List<String> lignesAConserver = new ArrayList<>();
              for (String ligne : Files.readAllLines(path)) {
                if (!ligne.trim().isEmpty()) {
                  String[] parties = ligne.split("\\|", 2);
                  if (parties.length >= 1) {
                    try {
                      LocalDateTime dateLigne = LocalDateTime.parse(parties[0],
                          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                      if (dateLigne.isAfter(limite)) {
                        lignesAConserver.add(ligne);
                      }
                    } catch (Exception e) {
                      // Si la ligne est mal formatée, on la garde quand même
                      lignesAConserver.add(ligne);
                    }
                  }
                }
              }

              // On réécrit le fichier avec seulement les messages récents
              Files.write(path, lignesAConserver, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
              System.err.println("Problème pour nettoyer " + path + " : " + e.getMessage());
            }
          });
    } catch (IOException e) {
      System.err.println("Problème pour nettoyer les messages : " + e.getMessage());
    }
  }

  /**
   * Exporte tout l'historique dans un fichier texte lisible
   */
  public static void exporterHistorique(String utilisateur, String fichierExport) {
    try {
      List<String> export = new ArrayList<>();
      export.add("=== HISTORIQUE DES CONVERSATIONS ===");
      export.add("Utilisateur : " + utilisateur);
      export.add("Date d'export : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
      export.add("");

      // Chat général d'abord
      export.add("--- CHAT GÉNÉRAL ---");
      export.addAll(chargerHistoriqueGeneral());
      export.add("");

      // Puis toutes les conversations privées
      List<String> conversations = obtenirConversationsPrivees(utilisateur);
      for (String autreUtilisateur : conversations) {
        export.add("--- CONVERSATION AVEC " + autreUtilisateur.toUpperCase() + " ---");
        export.addAll(chargerHistoriquePrive(utilisateur, autreUtilisateur));
        export.add("");
      }

      Files.write(Paths.get(fichierExport), export);
      System.out.println("Historique exporté vers : " + fichierExport);
    } catch (IOException e) {
      System.err.println("Problème pour exporter l'historique : " + e.getMessage());
    }
  }
}