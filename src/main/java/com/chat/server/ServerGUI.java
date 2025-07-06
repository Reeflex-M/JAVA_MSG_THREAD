package com.chat.server;

import com.chat.model.MessageStorage;
import com.chat.model.Utilisateur;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Interface graphique du serveur de chat
 * Permet de visualiser les clients connectés et les logs
 */
public class ServerGUI extends JFrame {

  // J'ai choisi ces couleurs pour avoir un look moderne mais pas trop flashy
  private static final Color PRIMARY_COLOR = new Color(33, 150, 243);
  private static final Color SECONDARY_COLOR = new Color(63, 81, 181);
  private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
  private static final Color WARNING_COLOR = new Color(255, 152, 0);
  private static final Color ERROR_COLOR = new Color(244, 67, 54);
  private static final Color BACKGROUND_COLOR = new Color(250, 250, 250);

  // Les composants principaux de l'interface
  private JPanel clientsPanel;
  private JLabel statusLabel;
  private JLabel statsLabel;
  private JTextArea logArea;
  private JButton startButton;
  private JButton stopButton;
  private JButton clearButton;
  private JLabel noClientsLabel;

  // Variables pour le serveur
  private Thread serverThread;
  private boolean isRunning = false;
  private static final int PORT = 1234; // Port par défaut, pourrait être configurable
  private static final String MESSAGES_FILE = "data/messages_offline.txt";

  // Stockage des données - on utilise maintenant celles d'EchoServer
  private static ConcurrentHashMap<String, ArrayList<String>> messagesHorsLigne = new ConcurrentHashMap<>();
  private ConcurrentHashMap<String, JPanel> panelsClients = new ConcurrentHashMap<>();

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      // Essaye d'utiliser Nimbus si possible, sinon on garde le look par défaut
      try {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
          if ("Nimbus".equals(info.getName())) {
            UIManager.setLookAndFeel(info.getClassName());
            break;
          }
        }
      } catch (Exception e) {
        // Pas grave si ça marche pas, on garde le look par défaut
      }
      new ServerGUI();
    });
  }

  public ServerGUI() {
    super("Serveur de Chat - Interface d'Administration");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(1200, 800);
    setLocationRelativeTo(null);

    // Initialisation des données au démarrage
    chargerMessagesHorsLigne();
    initializeUI();
    setVisible(true);

    // Associer cette interface avec EchoServer
    EchoServer.setServerGUI(this);

    // On démarre automatiquement le serveur après l'initialisation
    SwingUtilities.invokeLater(() -> {
      ajouterLog("Interface graphique prête");
      ajouterLog("Cliquez sur 'Démarrer' pour lancer le serveur");
      afficherMessageAucunClient();
    });
  }

  private void initializeUI() {
    setLayout(new BorderLayout(10, 10));

    // Panel principal avec un peu d'espacement
    JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
    mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
    mainPanel.setBackground(BACKGROUND_COLOR);

    // En-tête avec titre et boutons
    JPanel header = createHeader();
    mainPanel.add(header, BorderLayout.NORTH);

    // Division en 2 parties : clients à gauche, logs à droite
    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPane.setResizeWeight(0.6); // Un peu plus d'espace pour les clients
    splitPane.setBackground(BACKGROUND_COLOR);

    JPanel leftPanel = createClientsPanel();
    splitPane.setLeftComponent(leftPanel);

    JPanel rightPanel = createLogsPanel();
    splitPane.setRightComponent(rightPanel);

    mainPanel.add(splitPane, BorderLayout.CENTER);

    // Footer avec les stats
    JPanel footer = createFooter();
    mainPanel.add(footer, BorderLayout.SOUTH);

    add(mainPanel);
  }

  private JPanel createHeader() {
    JPanel header = new JPanel(new BorderLayout());
    header.setBackground(PRIMARY_COLOR);
    header.setBorder(new EmptyBorder(15, 20, 15, 20));

    JLabel title = new JLabel("Serveur de Chat Multi-Utilisateurs");
    title.setFont(new Font("Segoe UI", Font.BOLD, 24));
    title.setForeground(Color.WHITE);

    // Boutons de contrôle alignés à droite
    JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
    controlPanel.setOpaque(false);

    startButton = createStyledButton("Demarrer", SUCCESS_COLOR);
    stopButton = createStyledButton("Arreter", ERROR_COLOR);
    clearButton = createStyledButton("Effacer logs", WARNING_COLOR);

    // Connexion des evenements
    startButton.addActionListener(e -> demarrerServeur());
    stopButton.addActionListener(e -> arreterServeur());
    clearButton.addActionListener(e -> effacerLogs());

    stopButton.setEnabled(false); // Désactivé au début

    controlPanel.add(startButton);
    controlPanel.add(stopButton);
    controlPanel.add(clearButton);

    header.add(title, BorderLayout.WEST);
    header.add(controlPanel, BorderLayout.EAST);

    return header;
  }

  private JPanel createClientsPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
        "👥 Clients Connectés",
        TitledBorder.CENTER, TitledBorder.TOP,
        new Font("Segoe UI", Font.BOLD, 14), PRIMARY_COLOR));

    // Zone scrollable pour afficher les clients
    clientsPanel = new JPanel();
    clientsPanel.setLayout(new BoxLayout(clientsPanel, BoxLayout.Y_AXIS));
    clientsPanel.setBackground(Color.WHITE);

    JScrollPane scrollPane = new JScrollPane(clientsPanel);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setBorder(null);

    // Message quand aucun client n'est connecté
    if (messagesHorsLigne.isEmpty()) {
      noClientsLabel = new JLabel("Aucun client connecté");
      noClientsLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
      noClientsLabel.setForeground(Color.GRAY);
      noClientsLabel.setHorizontalAlignment(SwingConstants.CENTER);
      clientsPanel.add(noClientsLabel);
    }

    panel.add(scrollPane, BorderLayout.CENTER);
    return panel;
  }

  private JPanel createLogsPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(SECONDARY_COLOR, 2),
        "Logs du Serveur",
        TitledBorder.CENTER, TitledBorder.TOP,
        new Font("Segoe UI", Font.BOLD, 14), SECONDARY_COLOR));

    logArea = new JTextArea();
    logArea.setEditable(false);
    logArea.setFont(new Font("Consolas", Font.PLAIN, 12)); // Police monospace pour les logs
    logArea.setBackground(new Color(248, 248, 248));
    logArea.setBorder(new EmptyBorder(10, 10, 10, 10));

    JScrollPane scrollPane = new JScrollPane(logArea);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    panel.add(scrollPane, BorderLayout.CENTER);
    return panel;
  }

  private JPanel createFooter() {
    JPanel footer = new JPanel(new BorderLayout());
    footer.setBackground(BACKGROUND_COLOR);
    footer.setBorder(new EmptyBorder(15, 0, 0, 0));

    statusLabel = new JLabel("Serveur arrete");
    statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
    statusLabel.setForeground(ERROR_COLOR);

    statsLabel = new JLabel("Port: " + PORT + " | Clients: 0 | Messages en attente: 0");
    statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    statsLabel.setForeground(Color.GRAY);

    footer.add(statusLabel, BorderLayout.WEST);
    footer.add(statsLabel, BorderLayout.EAST);

    return footer;
  }

  // Méthode pour créer des boutons avec un style uniforme
  private JButton createStyledButton(String text, Color color) {
    JButton button = new JButton(text);
    button.setFont(new Font("Segoe UI", Font.BOLD, 12));
    button.setBackground(color);
    button.setForeground(Color.WHITE);
    button.setBorder(new EmptyBorder(10, 20, 10, 20));
    button.setFocusPainted(false);
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));

    // Petit effet hover pour rendre les boutons plus interactifs
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

  // --- Gestion du serveur ---

  private void demarrerServeur() {
    if (!isRunning) {
      // On lance EchoServer dans un thread séparé pour pas bloquer l'interface
      serverThread = new Thread(() -> {
        try {
          isRunning = true;

          // Mise à jour de l'interface sur le thread EDT
          SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Serveur demarre sur le port " + PORT);
            statusLabel.setForeground(SUCCESS_COLOR);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            ajouterLog("Serveur demarre sur le port " + PORT);
            afficherMessageAucunClient();
          });

          // Démarrer le serveur EchoServer avec sa propre logique
          ajouterLog("Démarrage du serveur EchoServer...");

          // Lancer EchoServer directement
          EchoServer.main(new String[0]);

        } catch (Exception e) {
          SwingUtilities.invokeLater(() -> {
            ajouterLog("Erreur generale : " + e.getMessage());
            statusLabel.setText("Erreur demarrage serveur");
            statusLabel.setForeground(ERROR_COLOR);
          });
        }
      });
      serverThread.start();
    }
  }

  private void arreterServeur() {
    if (isRunning) {
      isRunning = false;
      try {
        if (serverThread != null && serverThread.isAlive()) {
          serverThread.interrupt();
        }
      } catch (Exception e) {
        ajouterLog("Erreur lors de l'arrêt : " + e.getMessage());
      }

      statusLabel.setText("Serveur arrete");
      statusLabel.setForeground(ERROR_COLOR);
      startButton.setEnabled(true);
      stopButton.setEnabled(false);
      ajouterLog("Serveur arrete");

      // Vider la liste des clients
      panelsClients.clear();
      clientsPanel.removeAll();
      afficherMessageAucunClient();
    }
  }

  private void effacerLogs() {
    logArea.setText("");
    ajouterLog("Logs effacés");
  }

  // Ajoute un message dans la zone de logs avec timestamp
  private void ajouterLog(String message) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    logArea.append("[" + timestamp + "] " + message + "\n");
    // Auto-scroll vers le bas
    logArea.setCaretPosition(logArea.getDocument().getLength());
  }

  // Met à jour les statistiques affichées en bas
  private void mettreAJourStats() {
    int nbClients = EchoServer.utilisateursConnectes.size();
    int nbMessages = messagesHorsLigne.values().stream().mapToInt(ArrayList::size).sum();
    statsLabel.setText("Port: " + PORT + " | Clients: " + nbClients + " | Messages en attente: " + nbMessages);
  }

  // Charge les messages hors ligne depuis le fichier
  private void chargerMessagesHorsLigne() {
    try {
      if (Files.exists(Paths.get(MESSAGES_FILE))) {
        for (String ligne : Files.readAllLines(Paths.get(MESSAGES_FILE))) {
          if (!ligne.trim().isEmpty()) {
            String[] parties = ligne.split("\\|", 2);
            if (parties.length == 2) {
              String destinataire = parties[0];
              String message = parties[1];
              messagesHorsLigne.computeIfAbsent(destinataire, k -> new ArrayList<>()).add(message);
            }
          }
        }
      }
    } catch (IOException e) {
      ajouterLog("Erreur lors du chargement des messages : " + e.getMessage());
    }
  }

  // Méthodes de notification appelées par EchoServer
  public void notifierClientConnecte(String nom) {
    SwingUtilities.invokeLater(() -> {
      ajouterClientAListe(nom);
      ajouterLog("Client connecté : " + nom);
      mettreAJourStats();
    });
  }

  public void notifierClientDeconnecte(String nom) {
    SwingUtilities.invokeLater(() -> {
      retirerClientDeListe(nom);
      ajouterLog("Client déconnecté : " + nom);
      mettreAJourStats();
    });
  }

  public void notifierMessage(String expediteur, String message) {
    SwingUtilities.invokeLater(() -> {
      ajouterLog("Message de " + expediteur + " : " + message);
    });
  }

  public void notifierMessagePrive(String expediteur, String destinataire, String message) {
    SwingUtilities.invokeLater(() -> {
      ajouterLog("Message privé de " + expediteur + " vers " + destinataire + " : " + message);
    });
  }

  // Méthodes pour gérer l'affichage des clients
  private void ajouterClientAListe(String nom) {
    if (!panelsClients.containsKey(nom)) {
      // Cacher le message "Aucun client connecté"
      if (noClientsLabel != null) {
        noClientsLabel.setVisible(false);
      }

      // Créer un panel pour ce client
      JPanel clientPanel = new JPanel(new BorderLayout(10, 5));
      clientPanel.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createLineBorder(SUCCESS_COLOR, 1),
          new EmptyBorder(10, 15, 10, 15)));
      clientPanel.setBackground(Color.WHITE);
      clientPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

      // Icône et nom du client
      JLabel clientLabel = new JLabel("🟢 " + nom);
      clientLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
      clientLabel.setForeground(SUCCESS_COLOR);

      // Informations supplémentaires
      JLabel infoLabel = new JLabel(
          "Connecté - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
      infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
      infoLabel.setForeground(Color.GRAY);

      clientPanel.add(clientLabel, BorderLayout.WEST);
      clientPanel.add(infoLabel, BorderLayout.EAST);

      panelsClients.put(nom, clientPanel);
      clientsPanel.add(clientPanel);
      clientsPanel.revalidate();
      clientsPanel.repaint();
    }
  }

  private void retirerClientDeListe(String nom) {
    JPanel clientPanel = panelsClients.remove(nom);
    if (clientPanel != null) {
      clientsPanel.remove(clientPanel);
      clientsPanel.revalidate();
      clientsPanel.repaint();

      // Afficher le message "Aucun client connecté" si la liste est vide
      if (panelsClients.isEmpty()) {
        afficherMessageAucunClient();
      }
    }
  }

  private void afficherMessageAucunClient() {
    if (noClientsLabel == null) {
      noClientsLabel = new JLabel("Aucun client connecté");
      noClientsLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
      noClientsLabel.setForeground(Color.GRAY);
      noClientsLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }
    noClientsLabel.setVisible(true);
    clientsPanel.add(noClientsLabel);
    clientsPanel.revalidate();
    clientsPanel.repaint();
  }
}