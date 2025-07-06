package com.chat.launcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Lanceur principal de l'application de chat
 * Permet de choisir si on veut lancer le serveur ou le client
 */
public class ChatLauncher extends JFrame {

  private static final Color PRIMARY_COLOR = new Color(33, 150, 243);
  private static final Color SECONDARY_COLOR = new Color(63, 81, 181);
  private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
  private static final Color BACKGROUND_COLOR = new Color(250, 250, 250);

  public static void main(String[] args) {
    // On peut lancer directement en ligne de commande si on veut
    if (args.length > 0) {
      switch (args[0].toLowerCase()) {
        case "serveur":
        case "server":
          lancerServeur();
          return;
        case "client":
          lancerClient();
          return;
        default:
          System.out.println("Usage: java -jar chat-app.jar [serveur|client]");
          System.out.println("       java -jar chat-app.jar (pour l'interface graphique)");
          return;
      }
    }

    // Sinon on lance l'interface graphique de sélection
    SwingUtilities.invokeLater(() -> {
      try {
        // On essaie d'avoir un look sympa
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
          if ("Nimbus".equals(info.getName())) {
            UIManager.setLookAndFeel(info.getClassName());
            break;
          }
        }
      } catch (Exception e) {
        // Pas grave si ça marche pas
      }
      new ChatLauncher().setVisible(true);
    });
  }

  public ChatLauncher() {
    super("🚀 Lanceur Application de Chat");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(500, 400);
    setLocationRelativeTo(null);
    setResizable(false);

    initializeUI();
    creerRepertoiresNecessaires();
  }

  private void initializeUI() {
    setLayout(new BorderLayout());

    // Panel principal avec un fond propre
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBackground(BACKGROUND_COLOR);
    mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

    // En-tête avec titre
    JPanel header = new JPanel(new BorderLayout());
    header.setBackground(PRIMARY_COLOR);
    header.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    JLabel title = new JLabel("🚀 Application de Chat", SwingConstants.CENTER);
    title.setFont(new Font("Segoe UI", Font.BOLD, 24));
    title.setForeground(Color.WHITE);

    JLabel subtitle = new JLabel("Choisissez ce que vous voulez lancer", SwingConstants.CENTER);
    subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    subtitle.setForeground(Color.WHITE);

    header.add(title, BorderLayout.CENTER);
    header.add(subtitle, BorderLayout.SOUTH);

    // Zone avec les boutons
    JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 0, 20));
    buttonPanel.setBackground(BACKGROUND_COLOR);
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

    // Bouton pour lancer le serveur
    JButton serverButton = createLaunchButton("🖥️ Lancer le Serveur",
        "Interface d'administration du serveur",
        PRIMARY_COLOR);
    serverButton.addActionListener(e -> {
      dispose();
      lancerServeur();
    });

    // Bouton pour lancer le client
    JButton clientButton = createLaunchButton("💬 Lancer le Client",
        "Interface de chat pour discuter",
        SUCCESS_COLOR);
    clientButton.addActionListener(e -> {
      dispose();
      lancerClient();
    });

    // Bouton pour quitter
    JButton quitButton = createLaunchButton("❌ Quitter",
        "Fermer l'application",
        new Color(158, 158, 158));
    quitButton.addActionListener(e -> System.exit(0));

    buttonPanel.add(serverButton);
    buttonPanel.add(clientButton);
    buttonPanel.add(quitButton);

    // Footer avec info sur la version
    JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
    footer.setBackground(BACKGROUND_COLOR);
    JLabel footerText = new JLabel("Version 1.0 - Application de Chat Multi-Utilisateurs");
    footerText.setFont(new Font("Segoe UI", Font.ITALIC, 12));
    footerText.setForeground(Color.GRAY);
    footer.add(footerText);

    mainPanel.add(header, BorderLayout.NORTH);
    mainPanel.add(buttonPanel, BorderLayout.CENTER);
    mainPanel.add(footer, BorderLayout.SOUTH);

    add(mainPanel);
  }

  private JButton createLaunchButton(String text, String tooltip, Color color) {
    JButton button = new JButton(text);
    button.setToolTipText(tooltip);
    button.setFont(new Font("Segoe UI", Font.BOLD, 16));
    button.setBackground(color);
    button.setForeground(Color.WHITE);
    button.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
    button.setFocusPainted(false);
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));

    // Petit effet au survol pour faire plus interactif
    button.addMouseListener(new java.awt.event.MouseAdapter() {
      @Override
      public void mouseEntered(java.awt.event.MouseEvent e) {
        button.setBackground(color.brighter());
      }

      @Override
      public void mouseExited(java.awt.event.MouseEvent e) {
        button.setBackground(color);
      }
    });

    return button;
  }

  private void creerRepertoiresNecessaires() {
    try {
      // On crée les dossiers dont on a besoin
      Files.createDirectories(Paths.get("data"));
      Files.createDirectories(Paths.get("logs"));
      Files.createDirectories(Paths.get("data/messages_history"));

      // Et les fichiers de base s'ils n'existent pas encore
      if (!Files.exists(Paths.get("data/utilisateurs.txt"))) {
        Files.createFile(Paths.get("data/utilisateurs.txt"));
      }
      if (!Files.exists(Paths.get("data/messages_offline.txt"))) {
        Files.createFile(Paths.get("data/messages_offline.txt"));
      }
      if (!Files.exists(Paths.get("data/groupes_chat.txt"))) {
        Files.createFile(Paths.get("data/groupes_chat.txt"));
      }
    } catch (IOException e) {
      JOptionPane.showMessageDialog(this,
          "Problème lors de la création des dossiers : " + e.getMessage(),
          "Erreur d'initialisation",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private static void lancerServeur() {
    try {
      // On lance le serveur dans un processus séparé
      ProcessBuilder pb = new ProcessBuilder("java", "-cp", "target/classes", "com.chat.server.ServerGUI");
      pb.start();

      // On prévient l'utilisateur
      JOptionPane.showMessageDialog(null,
          "Le serveur a été lancé.\nVous pouvez maintenant connecter des clients.",
          "Serveur lancé",
          JOptionPane.INFORMATION_MESSAGE);

    } catch (IOException e) {
      JOptionPane.showMessageDialog(null,
          "Impossible de lancer le serveur : " + e.getMessage(),
          "Erreur",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private static void lancerClient() {
    try {
      // On lance le client dans un processus séparé
      ProcessBuilder pb = new ProcessBuilder("java", "-cp", "target/classes", "com.chat.client.ChatClientGUI");
      pb.start();

      // On prévient l'utilisateur
      JOptionPane.showMessageDialog(null,
          "Le client a été lancé.\nVous pouvez maintenant vous connecter au serveur.",
          "Client lancé",
          JOptionPane.INFORMATION_MESSAGE);

    } catch (IOException e) {
      JOptionPane.showMessageDialog(null,
          "Impossible de lancer le client : " + e.getMessage(),
          "Erreur",
          JOptionPane.ERROR_MESSAGE);
    }
  }
}