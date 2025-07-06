package com.chat.client;

import com.chat.model.Utilisateur;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.nio.file.*;

public class EchoClient {
    private static PrintWriter out;
    private static String utilisateurActuel;
    private static ArrayList<Utilisateur> utilisateurs;

    public static void main(String[] args) throws Exception {
        String serverAddress = "localhost";
        int port = 1234;

        // On charge d'abord la liste des utilisateurs qu'on a déjà
        utilisateurs = chargerUtilisateurs();
        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

        // L'utilisateur choisit son profil ou en crée un nouveau
        Utilisateur utilisateurSelectionne = selectionnerUtilisateur(utilisateurs, userInput);
        utilisateurActuel = utilisateurSelectionne.getNom();

        System.out.println("Connexion au serveur de chat...");

        try (Socket socket = new Socket(serverAddress, port)) {
            System.out.println("Connecté au serveur !");

            // On établit la communication avec le serveur
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Le serveur nous demande notre nom
            String serverMessage = in.readLine(); // "Entrez votre nom d'utilisateur :"
            System.out.println(serverMessage);
            out.println(utilisateurActuel);

            System.out.println("Connecté en tant que : " + utilisateurActuel);
            System.out.println("Commandes disponibles :");
            System.out.println("- /list : voir qui est connecté");
            System.out.println("- /msg <utilisateur> <message> : envoyer un message privé");
            System.out.println("- /change : changer de profil utilisateur");
            System.out.println("- /bye : quitter le chat");
            System.out.println("Tapez simplement votre message pour l'envoyer à tout le monde.");

            // On démarre un thread pour écouter les messages du serveur
            demarrerThreadReception(in);

            // Et on traite les messages que l'utilisateur tape
            traiterEntreeUtilisateur(userInput, socket);

        } catch (IOException e) {
            System.err.println("Problème de connexion au serveur : " + e.getMessage());
        }
    }

    // Charge les utilisateurs depuis le fichier (s'il existe)
    private static ArrayList<Utilisateur> chargerUtilisateurs() throws IOException {
        ArrayList<Utilisateur> utilisateurs = new ArrayList<>();
        if (Files.exists(Paths.get("data/utilisateurs.txt"))) {
            for (String ligne : Files.readAllLines(Paths.get("data/utilisateurs.txt"))) {
                if (!ligne.trim().isEmpty()) {
                    String[] parties = ligne.split(",");
                    if (parties.length >= 1) {
                        Utilisateur user = new Utilisateur(parties[0]);
                        if (parties.length >= 2) {
                            user.setAdresseIP(parties[1]);
                        }
                        utilisateurs.add(user);
                    }
                }
            }
        }
        return utilisateurs;
    }

    // L'utilisateur choisit son profil ou en crée un nouveau
    private static Utilisateur selectionnerUtilisateur(ArrayList<Utilisateur> utilisateurs, BufferedReader userInput)
            throws IOException {
        System.out.println("=== Sélection de votre profil ===");

        if (utilisateurs.isEmpty()) {
            System.out.println("Aucun utilisateur n'a été créé pour l'instant.");
            return creerNouvelUtilisateur(utilisateurs, userInput);
        }

        // On montre les profils disponibles
        System.out.println("Choisissez votre profil :");
        for (int i = 0; i < utilisateurs.size(); i++) {
            System.out.println((i + 1) + ". " + utilisateurs.get(i).getNom());
        }
        System.out.println((utilisateurs.size() + 1) + ". Créer un nouveau profil");
        System.out.print("Votre choix : ");

        // On traite la réponse
        int choix = Integer.parseInt(userInput.readLine()) - 1;
        if (choix == utilisateurs.size()) {
            return creerNouvelUtilisateur(utilisateurs, userInput);
        }

        if (choix >= 0 && choix < utilisateurs.size()) {
            return utilisateurs.get(choix);
        } else {
            System.out.println("Choix pas valide, on va créer un nouveau profil...");
            return creerNouvelUtilisateur(utilisateurs, userInput);
        }
    }

    // Créer un nouveau profil utilisateur
    private static Utilisateur creerNouvelUtilisateur(ArrayList<Utilisateur> utilisateurs, BufferedReader userInput)
            throws IOException {
        System.out.print("Choisissez votre nom d'utilisateur : ");
        String nom = userInput.readLine().trim();

        // Le nom ne peut pas être vide
        while (nom.isEmpty()) {
            System.out.print("Le nom ne peut pas être vide. Essayez encore : ");
            nom = userInput.readLine().trim();
        }

        // On vérifie que le nom n'est pas déjà pris
        for (Utilisateur u : utilisateurs) {
            if (u.getNom().equalsIgnoreCase(nom)) {
                System.out.println("Ce nom est déjà utilisé. Choisissez-en un autre.");
                return creerNouvelUtilisateur(utilisateurs, userInput);
            }
        }

        Utilisateur nouvelUtilisateur = new Utilisateur(nom);
        nouvelUtilisateur.setAdresseIP("localhost");
        utilisateurs.add(nouvelUtilisateur);

        // On sauvegarde le nouveau profil
        sauvegarderUtilisateurs(utilisateurs);
        System.out.println("Nouveau profil créé : " + nom);

        return nouvelUtilisateur;
    }

    // Sauvegarde la liste des utilisateurs dans le fichier
    private static void sauvegarderUtilisateurs(ArrayList<Utilisateur> utilisateurs) throws IOException {
        ArrayList<String> lignes = new ArrayList<>();
        for (Utilisateur u : utilisateurs) {
            lignes.add(u.getNom() + "," + (u.getAdresseIP() != null ? u.getAdresseIP() : "localhost"));
        }
        Files.write(Paths.get("data/utilisateurs.txt"), lignes);
    }

    // Thread qui écoute ce que le serveur nous envoie
    private static void demarrerThreadReception(BufferedReader in) {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    // On ignore le message initial de demande de nom
                    if (!message.equals("Entrez votre nom d'utilisateur :")) {
                        System.out.println(message);
                    }
                }
            } catch (IOException e) {
                System.out.println("Connexion au serveur perdue.");
                System.exit(0);
            }
        }).start();
    }

    // Traite ce que l'utilisateur tape et les commandes
    private static void traiterEntreeUtilisateur(BufferedReader userInput, Socket socket) throws IOException {
        String input;
        while ((input = userInput.readLine()) != null) {
            if (input.equals("/change")) {
                // Changer de profil utilisateur
                changerUtilisateur(userInput, socket);
            } else if (input.equals("/bye")) {
                // Quitter le chat
                out.println("/bye");
                System.out.println("À bientôt !");
                break;
            } else {
                // Envoyer le message au serveur
                out.println(input);
            }
        }
    }

    // Permet de changer de profil utilisateur en cours de session
    private static void changerUtilisateur(BufferedReader userInput, Socket socket) throws IOException {
        System.out.println("=== Changement de profil ===");

        // On recharge la liste des utilisateurs au cas où elle aurait changé
        utilisateurs = chargerUtilisateurs();

        // L'utilisateur choisit un autre profil
        Utilisateur nouvelUtilisateur = selectionnerUtilisateur(utilisateurs, userInput);
        String ancienUtilisateur = utilisateurActuel;
        utilisateurActuel = nouvelUtilisateur.getNom();

        // On informe le serveur du changement
        out.println("/change " + utilisateurActuel);

        System.out.println("Profil changé de " + ancienUtilisateur + " vers " + utilisateurActuel);
        System.out.println("Vous pouvez maintenant envoyer des messages sous ce nouveau profil.");
    }
}
