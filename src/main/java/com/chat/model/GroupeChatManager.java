package com.chat.model;

import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.*;
import java.nio.file.*;

/**
 * Gère les groupes de chat
 * Permet de créer des groupes, d'ajouter/virer des gens et de gérer qui peut
 * faire quoi
 */
public class GroupeChatManager {
  private static final String GROUPES_FILE = "data/groupes_chat.txt";
  private static Map<String, GroupeChat> groupes = new HashMap<>();
  private static Map<String, List<String>> utilisateursGroupes = new HashMap<>(); // qui est dans quels groupes

  /**
   * Un groupe de chat avec ses membres et modérateurs
   */
  public static class GroupeChat {
    private String nom;
    private String createur;
    private Set<String> membres;
    private Set<String> moderateurs;
    private LocalDateTime dateCreation;
    private boolean actif;
    private String description;

    public GroupeChat(String nom, String createur, String description) {
      this.nom = nom;
      this.createur = createur;
      this.description = description;
      this.membres = new HashSet<>();
      this.moderateurs = new HashSet<>();
      this.dateCreation = LocalDateTime.now();
      this.actif = true;

      // Celui qui crée le groupe devient automatiquement membre et modérateur
      this.membres.add(createur);
      this.moderateurs.add(createur);
    }

    // Les getters et setters classiques
    public String getNom() {
      return nom;
    }

    public String getCreateur() {
      return createur;
    }

    public Set<String> getMembres() {
      return membres;
    }

    public Set<String> getModerateurs() {
      return moderateurs;
    }

    public LocalDateTime getDateCreation() {
      return dateCreation;
    }

    public boolean isActif() {
      return actif;
    }

    public String getDescription() {
      return description;
    }

    public void setActif(boolean actif) {
      this.actif = actif;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public boolean estMembre(String utilisateur) {
      return membres.contains(utilisateur);
    }

    public boolean estModerateur(String utilisateur) {
      return moderateurs.contains(utilisateur);
    }

    public void ajouterMembre(String utilisateur) {
      membres.add(utilisateur);
    }

    public void supprimerMembre(String utilisateur) {
      membres.remove(utilisateur);
      // S'il était modérateur, on le vire aussi de là
      moderateurs.remove(utilisateur);
    }

    public void ajouterModerateur(String utilisateur) {
      if (membres.contains(utilisateur)) {
        moderateurs.add(utilisateur);
      }
    }

    public void supprimerModerateur(String utilisateur) {
      // On peut pas virer le créateur des modérateurs
      if (!utilisateur.equals(createur)) {
        moderateurs.remove(utilisateur);
      }
    }

    public String versLigneFichier() {
      return String.join("|",
          nom,
          createur,
          description,
          String.join(",", membres),
          String.join(",", moderateurs),
          dateCreation.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
          String.valueOf(actif));
    }

    public static GroupeChat depuisLigneFichier(String ligne) {
      String[] parties = ligne.split("\\|", -1);
      if (parties.length >= 7) {
        GroupeChat groupe = new GroupeChat(parties[0], parties[1], parties[2]);

        // On recharge les membres
        if (!parties[3].isEmpty()) {
          groupe.membres.addAll(Arrays.asList(parties[3].split(",")));
        }

        // Et les modérateurs
        if (!parties[4].isEmpty()) {
          groupe.moderateurs.addAll(Arrays.asList(parties[4].split(",")));
        }

        // Date de création
        try {
          groupe.dateCreation = LocalDateTime.parse(parties[5], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
          groupe.dateCreation = LocalDateTime.now();
        }

        // Si le groupe est actif ou pas
        groupe.actif = Boolean.parseBoolean(parties[6]);

        return groupe;
      }
      return null;
    }
  }

  /**
   * Charge tous les groupes depuis le fichier
   */
  public static void chargerGroupes() {
    try {
      if (Files.exists(Paths.get(GROUPES_FILE))) {
        for (String ligne : Files.readAllLines(Paths.get(GROUPES_FILE))) {
          if (!ligne.trim().isEmpty()) {
            GroupeChat groupe = GroupeChat.depuisLigneFichier(ligne);
            if (groupe != null) {
              groupes.put(groupe.getNom(), groupe);

              // On met à jour qui est dans quels groupes
              for (String membre : groupe.getMembres()) {
                utilisateursGroupes.computeIfAbsent(membre, k -> new ArrayList<>()).add(groupe.getNom());
              }
            }
          }
        }
      }
    } catch (IOException e) {
      System.err.println("Problème pour charger les groupes : " + e.getMessage());
    }
  }

  /**
   * Sauvegarde tous les groupes dans le fichier
   */
  public static void sauvegarderGroupes() {
    try {
      // On crée le dossier data s'il existe pas
      Files.createDirectories(Paths.get("data"));

      List<String> lignes = new ArrayList<>();
      for (GroupeChat groupe : groupes.values()) {
        lignes.add(groupe.versLigneFichier());
      }
      Files.write(Paths.get(GROUPES_FILE), lignes);
    } catch (IOException e) {
      System.err.println("Problème pour sauvegarder les groupes : " + e.getMessage());
    }
  }

  /**
   * Crée un nouveau groupe
   */
  public static boolean creerGroupe(String nomGroupe, String createur, String description) {
    if (groupes.containsKey(nomGroupe)) {
      return false; // Le groupe existe déjà
    }

    GroupeChat nouveauGroupe = new GroupeChat(nomGroupe, createur, description);
    groupes.put(nomGroupe, nouveauGroupe);

    // On met à jour la liste des groupes de l'utilisateur
    utilisateursGroupes.computeIfAbsent(createur, k -> new ArrayList<>()).add(nomGroupe);

    sauvegarderGroupes();
    return true;
  }

  /**
   * Ajoute quelqu'un à un groupe (seuls les modérateurs peuvent faire ça)
   */
  public static String ajouterUtilisateurAuGroupe(String nomGroupe, String utilisateur, String moderateur) {
    GroupeChat groupe = groupes.get(nomGroupe);
    if (groupe == null) {
      return "Le groupe '" + nomGroupe + "' n'existe pas";
    }

    if (!groupe.estModerateur(moderateur)) {
      return "Seuls les modérateurs peuvent ajouter des gens";
    }

    if (groupe.estMembre(utilisateur)) {
      return utilisateur + " est déjà dans le groupe";
    }

    groupe.ajouterMembre(utilisateur);
    utilisateursGroupes.computeIfAbsent(utilisateur, k -> new ArrayList<>()).add(nomGroupe);

    sauvegarderGroupes();
    return utilisateur + " a été ajouté au groupe '" + nomGroupe + "'";
  }

  /**
   * Vire quelqu'un d'un groupe (seuls les modérateurs peuvent faire ça)
   */
  public static String supprimerUtilisateurDuGroupe(String nomGroupe, String utilisateur, String moderateur) {
    GroupeChat groupe = groupes.get(nomGroupe);
    if (groupe == null) {
      return "Le groupe '" + nomGroupe + "' n'existe pas";
    }

    if (!groupe.estModerateur(moderateur)) {
      return "Seuls les modérateurs peuvent virer des gens";
    }

    if (utilisateur.equals(groupe.getCreateur())) {
      return "On peut pas virer le créateur du groupe";
    }

    if (!groupe.estMembre(utilisateur)) {
      return utilisateur + " n'est pas dans ce groupe";
    }

    groupe.supprimerMembre(utilisateur);
    List<String> groupesUtilisateur = utilisateursGroupes.get(utilisateur);
    if (groupesUtilisateur != null) {
      groupesUtilisateur.remove(nomGroupe);
    }

    sauvegarderGroupes();
    return utilisateur + " a été viré du groupe '" + nomGroupe + "'";
  }

  /**
   * Fait de quelqu'un un modérateur (seuls les autres modérateurs peuvent faire
   * ça)
   */
  public static String promouvoirModerateur(String nomGroupe, String utilisateur, String moderateur) {
    GroupeChat groupe = groupes.get(nomGroupe);
    if (groupe == null) {
      return "Le groupe '" + nomGroupe + "' n'existe pas";
    }

    if (!groupe.estModerateur(moderateur)) {
      return "Seuls les modérateurs peuvent promouvoir quelqu'un";
    }

    if (!groupe.estMembre(utilisateur)) {
      return utilisateur + " n'est pas membre du groupe";
    }

    if (groupe.estModerateur(utilisateur)) {
      return utilisateur + " est déjà modérateur";
    }

    groupe.ajouterModerateur(utilisateur);
    sauvegarderGroupes();
    return utilisateur + " est maintenant modérateur du groupe '" + nomGroupe + "'";
  }

  /**
   * Envoie un message à tout le groupe
   */
  public static void diffuserMessageGroupe(String nomGroupe, String expediteur, String message, Object serverInstance) {
    GroupeChat groupe = groupes.get(nomGroupe);
    if (groupe != null && groupe.isActif() && groupe.estMembre(expediteur)) {
      String messageFormate = "[" + nomGroupe + "] " + expediteur + " : " + message;

      // TODO: Ici il faudrait envoyer le message à tous les membres connectés
      // Pour l'instant on fait juste une sauvegarde
      MessageStorage.sauvegarderMessageGroupe(nomGroupe, expediteur, message);
    }
  }

  /**
   * Donne la liste des groupes où quelqu'un est membre
   */
  public static List<String> obtenirGroupesUtilisateur(String utilisateur) {
    return utilisateursGroupes.getOrDefault(utilisateur, new ArrayList<>());
  }

  /**
   * Récupère un groupe par son nom
   */
  public static GroupeChat obtenirGroupe(String nomGroupe) {
    return groupes.get(nomGroupe);
  }

  /**
   * Récupère tous les groupes
   */
  public static Map<String, GroupeChat> obtenirTousGroupes() {
    return groupes;
  }

  /**
   * Vérifie si quelqu'un est membre d'un groupe
   */
  public static boolean estMembreGroupe(String nomGroupe, String utilisateur) {
    GroupeChat groupe = groupes.get(nomGroupe);
    return groupe != null && groupe.estMembre(utilisateur);
  }

  /**
   * Vérifie si quelqu'un est modérateur d'un groupe
   */
  public static boolean estModerateurGroupe(String nomGroupe, String utilisateur) {
    GroupeChat groupe = groupes.get(nomGroupe);
    return groupe != null && groupe.estModerateur(utilisateur);
  }

  /**
   * Aide sur les commandes de groupes
   */
  public static String obtenirAideGroupes() {
    return "=== COMMANDES GROUPES ===\n" +
        "/groupe creer <nom> <description> : créer un nouveau groupe\n" +
        "/groupe rejoindre <nom> : demander à rejoindre un groupe\n" +
        "/groupe quitter <nom> : quitter un groupe\n" +
        "/groupe liste : voir tous les groupes\n" +
        "/groupe info <nom> : infos sur un groupe\n" +
        "/groupe inviter <groupe> <utilisateur> : inviter quelqu'un (modérateurs)\n" +
        "/groupe virer <groupe> <utilisateur> : virer quelqu'un (modérateurs)\n" +
        "/groupe moderateur <groupe> <utilisateur> : promouvoir modérateur\n";
  }
}