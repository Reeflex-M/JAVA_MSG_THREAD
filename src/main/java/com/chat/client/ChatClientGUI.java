package com.chat.client;

import com.chat.model.Utilisateur;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.*;

/**
 * Interface graphique du client de chat
 * Version avec une interface moderne et plein de fonctionnalités
 */
public class ChatClientGUI extends JFrame {
  // J'ai pris les couleurs Material Design, ça fait plus propre
  private static final Color PRIMARY_COLOR = new Color(33, 150, 243);
  private static final Color SECONDARY_COLOR = new Color(63, 81, 181);
  private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
  private static final Color WARNING_COLOR = new Color(255, 152, 0);
  private static final Color ERROR_COLOR = new Color(244, 67, 54);
  private static final Color BACKGROUND_COLOR = new Color(250, 250, 250);

  // Tout ce qu'il faut pour la connexion
  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  private String utilisateurActuel;
  private ArrayList<Utilisateur> utilisateurs;

  // Les composants de l'interface
  private JTabbedPane tabbedPane;
  private JTextArea chatGeneralArea;
  private JTextField messageField;
  private JButton sendButton;
  private JList<String> userList;
  private DefaultListModel<String> userListModel;
  private JLabel statusLabel;
  private JLabel userInfoLabel;

  // Composants pour les groupes
  private JList<String> groupList;
  private DefaultListModel<String> groupListModel;
  private JTextArea groupInfoArea;
  private JTextField groupNameField;
  private JTextField groupDescriptionField;
  private JComboBox<String> groupMemberCombo;

  // Pour gérer l'état
  private boolean isConnected = false;
  private Map<String, JTextArea> conversationsPrivees = new HashMap<>();

  // Pour accumuler les messages de groupes
  private boolean collectingGroupMessages = false;
  private StringBuilder groupMessageBuilder = new StringBuilder();

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      try {
        // On essaie d'avoir un look sympa
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
          if ("Nimbus".equals(info.getName())) {
            UIManager.setLookAndFeel(info.getClassName());
            break;
          }
        }
        new ChatClientGUI();
      } catch (Exception e) {
        new ChatClientGUI();
      }
    });
  }

  public ChatClientGUI() {
    super("Chat Multi-Utilisateurs - Interface Moderne");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(1000, 700);
    setLocationRelativeTo(null);

    initializeModernUI();

    try {
      utilisateurs = chargerUtilisateurs();
      utilisateurActuel = choisirUtilisateur();
      if (utilisateurActuel != null) {
        connecterAuServeur();
        setVisible(true);
      } else {
        dispose();
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, "Problème : " + e.getMessage());
      dispose();
    }
  }

  private void initializeModernUI() {
    setLayout(new BorderLayout(10, 10));

    // Panel principal avec un fond moderne
    JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
    mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
    mainPanel.setBackground(BACKGROUND_COLOR);

    // En-tête avec un gradient qui claque
    JPanel header = createModernHeader();
    mainPanel.add(header, BorderLayout.NORTH);

    // Contenu principal
    JPanel content = new JPanel(new BorderLayout(15, 0));
    content.setBackground(BACKGROUND_COLOR);

    // Zone de chat avec des onglets
    tabbedPane = new JTabbedPane();
    tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));

    chatGeneralArea = createModernTextArea();
    JScrollPane chatScroll = new JScrollPane(chatGeneralArea);
    chatScroll.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
        "💬 Discussion Générale",
        TitledBorder.LEFT, TitledBorder.TOP,
        new Font("Segoe UI", Font.BOLD, 12), PRIMARY_COLOR));

    tabbedPane.addTab("🏠 Chat Général", chatScroll);

    // Onglet des groupes
    JPanel groupPanel = createGroupPanel();
    tabbedPane.addTab("👥 Groupes", groupPanel);

    content.add(tabbedPane, BorderLayout.CENTER);

    // Panel des utilisateurs sur le côté
    JPanel userPanel = createModernUserPanel();
    content.add(userPanel, BorderLayout.EAST);

    mainPanel.add(content, BorderLayout.CENTER);

    // Footer avec le champ pour taper
    JPanel footer = createModernFooter();
    mainPanel.add(footer, BorderLayout.SOUTH);

    add(mainPanel);
  }

  private JPanel createModernHeader() {
    JPanel header = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint gradient = new GradientPaint(0, 0, PRIMARY_COLOR, getWidth(), 0, SECONDARY_COLOR);
        g2d.setPaint(gradient);
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
        g2d.dispose();
      }
    };
    header.setLayout(new BorderLayout());
    header.setPreferredSize(new Dimension(0, 70));
    header.setBorder(new EmptyBorder(20, 25, 20, 25));

    JLabel title = new JLabel("Chat Multi-Utilisateurs Moderne");
    title.setFont(new Font("Segoe UI", Font.BOLD, 22));
    title.setForeground(Color.WHITE);

    JButton aboutBtn = createModernButton("A propos", new Color(255, 255, 255, 50));
    aboutBtn.addActionListener(e -> showAbout());

    header.add(title, BorderLayout.WEST);
    header.add(aboutBtn, BorderLayout.EAST);

    return header;
  }

  private JPanel createModernUserPanel() {
    JPanel panel = new JPanel(new BorderLayout(10, 10));
    panel.setPreferredSize(new Dimension(280, 0));
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(SUCCESS_COLOR, 2),
        "Autres Utilisateurs Connectes",
        TitledBorder.CENTER, TitledBorder.TOP,
        new Font("Segoe UI", Font.BOLD, 12), SUCCESS_COLOR));

    // Liste des utilisateurs avec des indicateurs de statut
    userListModel = new DefaultListModel<>();
    userList = new JList<>(userListModel);
    userList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    userList.setBackground(new Color(248, 250, 252));
    userList.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Petit truc pour afficher le statut avec des couleurs
    userList.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value,
          int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        String userText = value.toString();
        if (userText.startsWith("🟢 ")) {
          setForeground(isSelected ? Color.WHITE : new Color(34, 139, 34));
        } else if (userText.startsWith("🔴 ")) {
          setForeground(isSelected ? Color.WHITE : new Color(139, 69, 19));
        }

        return this;
      }
    });

    userList.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          envoyerMessagePrive();
        }
      }
    });

    JScrollPane userScroll = new JScrollPane(userList);
    userScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

    // Panel pour afficher l'utilisateur connecté
    JPanel userInfoPanel = new JPanel(new BorderLayout());
    userInfoPanel.setBackground(new Color(240, 248, 255));
    userInfoPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
        new EmptyBorder(10, 10, 10, 10)));

    userInfoLabel = new JLabel("Non connecte");
    userInfoLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
    userInfoLabel.setForeground(PRIMARY_COLOR);
    userInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);
    userInfoPanel.add(userInfoLabel, BorderLayout.CENTER);

    panel.add(userInfoPanel, BorderLayout.NORTH);
    panel.add(userScroll, BorderLayout.CENTER);

    // Boutons pour les actions
    JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 5, 5));
    buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

    JButton msgPriveBtn = createModernButton("💬 Message privé", PRIMARY_COLOR);
    msgPriveBtn.addActionListener(e -> envoyerMessagePrive());

    JButton msgHorsLigneBtn = createModernButton("📤 Message hors ligne", WARNING_COLOR);
    msgHorsLigneBtn.addActionListener(e -> envoyerMessageHorsLigne());

    JButton refreshBtn = createModernButton("🔄 Actualiser", SECONDARY_COLOR);
    refreshBtn.addActionListener(e -> rafraichirListeUtilisateurs());

    buttonPanel.add(msgPriveBtn);
    buttonPanel.add(msgHorsLigneBtn);
    buttonPanel.add(refreshBtn);

    panel.add(buttonPanel, BorderLayout.SOUTH);

    return panel;
  }

  private JPanel createModernFooter() {
    JPanel footer = new JPanel(new BorderLayout(15, 10));
    footer.setBackground(BACKGROUND_COLOR);
    footer.setBorder(new EmptyBorder(15, 0, 0, 0));

    messageField = new JTextField();
    messageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    messageField.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
        new EmptyBorder(10, 15, 10, 15)));
    messageField.addActionListener(e -> envoyerMessage());

    // Panel pour les boutons
    JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
    buttonPanel.setBackground(BACKGROUND_COLOR);

    sendButton = createModernButton("📤 Envoyer", SUCCESS_COLOR);
    sendButton.setPreferredSize(new Dimension(120, 45));
    sendButton.addActionListener(e -> envoyerMessage());

    JButton disconnectButton = createModernButton("🔌 Déconnexion", ERROR_COLOR);
    disconnectButton.setPreferredSize(new Dimension(120, 45));
    disconnectButton.addActionListener(e -> deconnecter());

    buttonPanel.add(sendButton);
    buttonPanel.add(disconnectButton);

    statusLabel = new JLabel("Connectez-vous pour commencer");
    statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
    statusLabel.setForeground(new Color(108, 117, 125));

    footer.add(messageField, BorderLayout.CENTER);
    footer.add(buttonPanel, BorderLayout.EAST);
    footer.add(statusLabel, BorderLayout.SOUTH);

    return footer;
  }

  private JTextArea createModernTextArea() {
    JTextArea textArea = new JTextArea();
    textArea.setEditable(false);
    textArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    textArea.setBackground(Color.WHITE);
    textArea.setBorder(new EmptyBorder(15, 15, 15, 15));
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    return textArea;
  }

  private JButton createModernButton(String text, Color color) {
    JButton button = new JButton(text);
    button.setFont(new Font("Segoe UI", Font.BOLD, 12));
    button.setBackground(color);
    button.setForeground(Color.WHITE);
    button.setBorder(new EmptyBorder(8, 16, 8, 16));
    button.setFocusPainted(false);
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));

    button.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        button.setBackground(color.brighter());
      }

      @Override
      public void mouseExited(MouseEvent e) {
        button.setBackground(color);
      }
    });

    return button;
  }

  private JPanel createGroupPanel() {
    JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
    mainPanel.setBackground(BACKGROUND_COLOR);
    mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

    // Panel de gauche - Liste des groupes
    JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
    leftPanel.setPreferredSize(new Dimension(300, 0));
    leftPanel.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
        "👥 Mes Groupes",
        TitledBorder.LEFT, TitledBorder.TOP,
        new Font("Segoe UI", Font.BOLD, 12), PRIMARY_COLOR));

    groupListModel = new DefaultListModel<>();
    groupList = new JList<>(groupListModel);
    groupList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    groupList.setBackground(new Color(248, 250, 252));
    groupList.setBorder(new EmptyBorder(10, 10, 10, 10));
    groupList.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        String selectedGroup = groupList.getSelectedValue();
        if (selectedGroup != null) {
          afficherInfoGroupe(selectedGroup);
        }
      }
    });

    JScrollPane groupScroll = new JScrollPane(groupList);
    leftPanel.add(groupScroll, BorderLayout.CENTER);

    // Boutons d'action pour les groupes
    JPanel groupButtonPanel = new JPanel(new GridLayout(2, 1, 5, 5));
    groupButtonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

    JButton refreshGroupsBtn = createModernButton("🔄 Actualiser", SECONDARY_COLOR);
    refreshGroupsBtn.addActionListener(e -> {
      rafraichirListeGroupes();
      rafraichirListeUtilisateurs(); // Actualiser aussi les utilisateurs
      SwingUtilities.invokeLater(() -> {
        try {
          Thread.sleep(500); // Attendre que les listes soient mises à jour
          mettreAJourComboUtilisateurs(); // Mettre à jour le combo
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      });
    });

    JButton myGroupsBtn = createModernButton("📋 Mes groupes", PRIMARY_COLOR);
    myGroupsBtn.addActionListener(e -> {
      voirMesGroupes();
      rafraichirListeUtilisateurs(); // Actualiser aussi les utilisateurs
      SwingUtilities.invokeLater(() -> {
        try {
          Thread.sleep(500); // Attendre que les listes soient mises à jour
          mettreAJourComboUtilisateurs(); // Mettre à jour le combo
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      });
    });

    groupButtonPanel.add(refreshGroupsBtn);
    groupButtonPanel.add(myGroupsBtn);

    leftPanel.add(groupButtonPanel, BorderLayout.SOUTH);

    // Panel de droite - Gestion des groupes
    JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
    rightPanel.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(SUCCESS_COLOR, 2),
        "🛠️ Gestion des Groupes",
        TitledBorder.LEFT, TitledBorder.TOP,
        new Font("Segoe UI", Font.BOLD, 12), SUCCESS_COLOR));

    // Zone d'informations du groupe
    groupInfoArea = createModernTextArea();
    groupInfoArea.setText("Sélectionnez un groupe pour voir ses informations...");
    JScrollPane infoScroll = new JScrollPane(groupInfoArea);
    infoScroll.setPreferredSize(new Dimension(0, 200));
    rightPanel.add(infoScroll, BorderLayout.CENTER);

    // Panel de création/gestion
    JPanel actionPanel = new JPanel(new GridBagLayout());
    actionPanel.setBackground(BACKGROUND_COLOR);
    actionPanel.setBorder(new EmptyBorder(15, 0, 0, 0));
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    // Création d'un nouveau groupe
    gbc.gridx = 0;
    gbc.gridy = 0;
    actionPanel.add(new JLabel("Nom du groupe:"), gbc);
    gbc.gridx = 1;
    gbc.gridy = 0;
    groupNameField = new JTextField(15);
    actionPanel.add(groupNameField, gbc);

    gbc.gridx = 0;
    gbc.gridy = 1;
    actionPanel.add(new JLabel("Description:"), gbc);
    gbc.gridx = 1;
    gbc.gridy = 1;
    groupDescriptionField = new JTextField(15);
    actionPanel.add(groupDescriptionField, gbc);

    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.gridwidth = 2;
    JButton createGroupBtn = createModernButton("✨ Créer un groupe", SUCCESS_COLOR);
    createGroupBtn.addActionListener(e -> creerNouveauGroupe());
    actionPanel.add(createGroupBtn, gbc);

    // Gestion des membres
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 1;
    actionPanel.add(new JLabel("Utilisateur:"), gbc);
    gbc.gridx = 1;
    gbc.gridy = 3;
    groupMemberCombo = new JComboBox<>();
    groupMemberCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    groupMemberCombo.addItem("🔽 Sélectionner un utilisateur...");
    actionPanel.add(groupMemberCombo, gbc);

    JPanel memberButtonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
    memberButtonPanel.setBackground(BACKGROUND_COLOR);

    JButton addMemberBtn = createModernButton("➕ Ajouter", PRIMARY_COLOR);
    addMemberBtn.addActionListener(e -> ajouterMembreGroupe());

    JButton removeMemberBtn = createModernButton("➖ Supprimer", ERROR_COLOR);
    removeMemberBtn.addActionListener(e -> supprimerMembreGroupe());

    memberButtonPanel.add(addMemberBtn);
    memberButtonPanel.add(removeMemberBtn);

    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.gridwidth = 2;
    actionPanel.add(memberButtonPanel, gbc);

    rightPanel.add(actionPanel, BorderLayout.SOUTH);

    mainPanel.add(leftPanel, BorderLayout.WEST);
    mainPanel.add(rightPanel, BorderLayout.CENTER);

    return mainPanel;
  }

  private void showAbout() {
    JDialog dialog = new JDialog(this, "À propos", true);
    dialog.setSize(400, 300);
    dialog.setLocationRelativeTo(this);

    JPanel content = new JPanel(new BorderLayout(15, 15));
    content.setBorder(new EmptyBorder(20, 20, 20, 20));
    content.setBackground(Color.WHITE);

    JLabel title = new JLabel("💬 Chat Moderne", SwingConstants.CENTER);
    title.setFont(new Font("Segoe UI", Font.BOLD, 18));
    title.setForeground(PRIMARY_COLOR);

    JTextArea info = new JTextArea(
        "🚀 Version 2.0 - Interface Moderne\n\n" +
            "✨ Fonctionnalités :\n" +
            "• Design moderne Material\n" +
            "• Messages privés avec onglets\n" +
            "• Interface responsive\n" +
            "• Effets visuels modernes\n\n" +
            "🛠️ Java Swing moderne\n" +
            "👨‍💻 Développé avec passion ❤️");
    info.setEditable(false);
    info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    info.setBackground(Color.WHITE);

    JButton closeBtn = createModernButton("✖️ Fermer", ERROR_COLOR);
    closeBtn.addActionListener(e -> dialog.dispose());

    content.add(title, BorderLayout.NORTH);
    content.add(info, BorderLayout.CENTER);
    content.add(closeBtn, BorderLayout.SOUTH);

    dialog.add(content);
    dialog.setVisible(true);
  }

  // Méthodes de fonctionnalité (simplifiées)
  private ArrayList<Utilisateur> chargerUtilisateurs() throws IOException {
    ArrayList<Utilisateur> users = new ArrayList<>();
    if (Files.exists(Paths.get("data/utilisateurs.txt"))) {
      for (String ligne : Files.readAllLines(Paths.get("data/utilisateurs.txt"))) {
        if (!ligne.trim().isEmpty()) {
          String[] parties = ligne.split(",");
          if (parties.length >= 1) {
            Utilisateur user = new Utilisateur(parties[0]);
            if (parties.length >= 2) {
              user.setAdresseIP(parties[1]);
            }
            users.add(user);
          }
        }
      }
    }
    return users;
  }

  private String choisirUtilisateur() {
    if (utilisateurs.isEmpty()) {
      return creerNouvelUtilisateur();
    }

    String[] options = { "Utilisateur existant", "Nouveau profil" };
    int choix = JOptionPane.showOptionDialog(this,
        "Choisissez votre mode :", "Connexion",
        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
        null, options, options[0]);

    if (choix == 0) {
      String[] noms = utilisateurs.stream().map(Utilisateur::getNom).toArray(String[]::new);
      return (String) JOptionPane.showInputDialog(this,
          "Sélectionnez :", "Utilisateur", JOptionPane.QUESTION_MESSAGE,
          null, noms, noms[0]);
    } else {
      return creerNouvelUtilisateur();
    }
  }

  private String creerNouvelUtilisateur() {
    String nom = JOptionPane.showInputDialog(this, "Nom d'utilisateur :");
    if (nom != null && !nom.trim().isEmpty()) {
      nom = nom.trim();
      Utilisateur nouveau = new Utilisateur(nom);
      nouveau.setAdresseIP("localhost");
      utilisateurs.add(nouveau);
      try {
        sauvegarderUtilisateurs();
        return nom;
      } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "Erreur sauvegarde : " + e.getMessage());
      }
    }
    return null;
  }

  private void sauvegarderUtilisateurs() throws IOException {
    // Créer le répertoire data s'il n'existe pas
    Files.createDirectories(Paths.get("data"));

    ArrayList<String> lignes = new ArrayList<>();
    for (Utilisateur u : utilisateurs) {
      lignes.add(u.getNom() + "," + (u.getAdresseIP() != null ? u.getAdresseIP() : "localhost"));
    }
    Files.write(Paths.get("data/utilisateurs.txt"), lignes);
  }

  private void connecterAuServeur() {
    try {
      socket = new Socket("localhost", 1234);
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      String serverPrompt = in.readLine();
      out.println(utilisateurActuel);

      isConnected = true;
      statusLabel.setText("Connecte : " + utilisateurActuel);
      statusLabel.setForeground(SUCCESS_COLOR);
      userInfoLabel.setText("Connecte en tant que : " + utilisateurActuel);
      messageField.setEnabled(true);
      sendButton.setEnabled(true);

      new Thread(() -> {
        try {
          String message;
          while ((message = in.readLine()) != null) {
            final String msg = message;
            SwingUtilities.invokeLater(() -> traiterMessage(msg));
          }
        } catch (IOException e) {
          SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Connexion perdue");
            statusLabel.setForeground(ERROR_COLOR);
            userInfoLabel.setText("Deconnecte");
            isConnected = false;
          });
        }
      }).start();

      // Charger automatiquement les groupes et utilisateurs après connexion
      SwingUtilities.invokeLater(() -> {
        try {
          Thread.sleep(1000); // Attendre que la connexion soit établie
          rafraichirListeUtilisateurs(); // Charger la liste des utilisateurs
          rafraichirListeGroupes();
          voirMesGroupes();
          Thread.sleep(500); // Attendre un peu plus pour les utilisateurs
          mettreAJourComboUtilisateurs(); // Mettre à jour le combo
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });

    } catch (IOException e) {
      JOptionPane.showMessageDialog(this, "Erreur connexion : " + e.getMessage());
      statusLabel.setText("Echec connexion");
      statusLabel.setForeground(ERROR_COLOR);
      userInfoLabel.setText("Connexion echouee");
    }
  }

  private void traiterMessage(String message) {
    if (message.equals("Entrez votre nom d'utilisateur :"))
      return;

    // Debug: afficher tous les messages reçus du serveur
    System.out.println("DEBUG - Message reçu du serveur: '" + message + "'");

    if (message.startsWith("[PRIVÉ]")) {
      traiterMessagePrive(message);
    } else if (message.startsWith("Utilisateurs connectés")) {
      mettreAJourListeUtilisateurs(message);
    } else if (message.contains("créé avec succès") || message.contains("ajouté au groupe") ||
        message.contains("viré du groupe") || message.contains("supprimé du groupe")) {
      // Actualiser la liste des groupes après une action sur les groupes
      ajouterMessageGeneral(message);
      SwingUtilities.invokeLater(() -> {
        rafraichirListeGroupes();
        voirMesGroupes();
      });
    } else if (message.startsWith("=== LISTE DES GROUPES ===") ||
        message.startsWith("=== VOS GROUPES ===")) {
      // Commencer à collecter les messages de groupes
      System.out.println("DEBUG - Début de collecte de groupes");
      collectingGroupMessages = true;
      groupMessageBuilder.setLength(0); // Reset
      groupMessageBuilder.append(message).append("\n");
      ajouterMessageGeneral(message);
      // Vider la liste des groupes pour la nouvelle liste
      SwingUtilities.invokeLater(() -> groupListModel.clear());
    } else if (collectingGroupMessages && message.trim().startsWith("- ")) {
      // Ligne de groupe
      System.out.println("DEBUG - Ligne de groupe: " + message);
      groupMessageBuilder.append(message).append("\n");
      ajouterMessageGeneral(message);
      // Traiter cette ligne de groupe immédiatement
      traiterLigneGroupe(message);
    } else if (message.startsWith("=== MEMBRES DU GROUPE")) {
      // Commencer à collecter les informations du groupe
      System.out.println("DEBUG - Début de collecte d'infos de groupe");
      collectingGroupMessages = true;
      groupMessageBuilder.setLength(0); // Reset
      groupMessageBuilder.append(message).append("\n");
      ajouterMessageGeneral(message);
    } else if (collectingGroupMessages && (message.startsWith("Créateur :") ||
        message.startsWith("Modérateurs :") || message.startsWith("Membres :") ||
        message.startsWith("Total :"))) {
      // Lignes d'informations du groupe
      System.out.println("DEBUG - Ligne d'info groupe: " + message);
      groupMessageBuilder.append(message).append("\n");
      ajouterMessageGeneral(message);
      // Mettre à jour l'affichage en temps réel
      mettreAJourInfoGroupe(groupMessageBuilder.toString());
    } else if (message.equals("Aucun groupe n'a été créé pour le moment.") ||
        message.equals("Vous n'êtes membre d'aucun groupe.")) {
      // Messages d'absence de groupes
      collectingGroupMessages = false;
      ajouterMessageGeneral(message);
      SwingUtilities.invokeLater(() -> {
        groupListModel.clear();
        groupInfoArea.setText("Aucun groupe trouvé.\nCréez votre premier groupe ci-dessous !");
      });
    } else {
      // Si on était en train de collecter des groupes et qu'on reçoit autre chose, on
      // arrête
      if (collectingGroupMessages) {
        System.out.println("DEBUG - Fin de collecte de groupes");
        collectingGroupMessages = false;
      }
      ajouterMessageGeneral(message);
    }
  }

  private void traiterMessagePrive(String message) {
    try {
      String contenu = message.substring("[PRIVÉ] ".length());
      int fleche = contenu.indexOf(" ? ");
      if (fleche != -1) {
        String expediteur = contenu.substring(0, fleche);
        String reste = contenu.substring(fleche + 3);
        int deuxPoints = reste.indexOf(" : ");
        if (deuxPoints != -1) {
          String destinataire = reste.substring(0, deuxPoints);
          String autreUtilisateur = expediteur.equals(utilisateurActuel) ? destinataire : expediteur;

          creerOngletConversation(autreUtilisateur);
          ajouterMessageConversation(autreUtilisateur, message);
        }
      }
    } catch (Exception e) {
      ajouterMessageGeneral(message);
    }
  }

  private void mettreAJourListeUtilisateurs(String listeMessage) {
    SwingUtilities.invokeLater(() -> {
      userListModel.clear();
      if (listeMessage.startsWith("Utilisateurs connectés : ")) {
        String users = listeMessage.substring("Utilisateurs connectés : ".length());
        String[] userArray = users.split(", ");
        for (String user : userArray) {
          if (!user.trim().isEmpty() && !user.trim().equals(utilisateurActuel)) {
            // Ajouter avec indicateur vert pour les connectés, en excluant l'utilisateur
            // actuel
            userListModel.addElement("Connecte : " + user.trim());
          }
        }
      } else if (listeMessage.equals("Aucun utilisateur connecté")) {
        // Si aucun utilisateur connecté, afficher quand même les utilisateurs connus
        // comme déconnectés
        for (Utilisateur user : utilisateurs) {
          if (!user.getNom().equals(utilisateurActuel)) { // Ne pas s'afficher soi-même
            userListModel.addElement("Hors ligne : " + user.getNom());
          }
        }
      }

      // Mettre à jour le combobox des groupes avec les utilisateurs disponibles
      System.out.println("DEBUG - Nombre d'utilisateurs dans la liste: " + userListModel.getSize());
      for (int i = 0; i < userListModel.getSize(); i++) {
        System.out.println("DEBUG - Utilisateur " + i + ": '" + userListModel.getElementAt(i) + "'");
      }
      mettreAJourComboUtilisateurs();

      // Mettre à jour le statut
      int connectes = userListModel.getSize();

      String statusText = "";
      if (connectes == 0) {
        statusText = "Aucun autre utilisateur connecte";
      } else if (connectes == 1) {
        statusText = "1 autre utilisateur connecte";
      } else {
        statusText = connectes + " autres utilisateurs connectes";
      }

      statusLabel.setText(statusText);
    });
  }

  private void ajouterMessageGeneral(String message) {
    String timestamp = java.time.LocalDateTime.now().format(
        java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    chatGeneralArea.append("[" + timestamp + "] " + message + "\n");
    chatGeneralArea.setCaretPosition(chatGeneralArea.getDocument().getLength());
  }

  private void creerOngletConversation(String utilisateur) {
    if (!conversationsPrivees.containsKey(utilisateur)) {
      JTextArea areaConv = createModernTextArea();
      conversationsPrivees.put(utilisateur, areaConv);

      JScrollPane scroll = new JScrollPane(areaConv);
      scroll.setBorder(BorderFactory.createLineBorder(SECONDARY_COLOR, 1));
      tabbedPane.addTab("💬 " + utilisateur, scroll);
    }
  }

  private void ajouterMessageConversation(String utilisateur, String message) {
    JTextArea area = conversationsPrivees.get(utilisateur);
    if (area != null) {
      String timestamp = java.time.LocalDateTime.now().format(
          java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
      area.append("[" + timestamp + "] " + message + "\n");
      area.setCaretPosition(area.getDocument().getLength());
    }
  }

  private void envoyerMessage() {
    String message = messageField.getText().trim();
    if (!message.isEmpty() && isConnected) {
      int selectedTab = tabbedPane.getSelectedIndex();
      if (selectedTab == 0) {
        ajouterMessageGeneral(utilisateurActuel + " : " + message);
        out.println(message);
      } else {
        String titre = tabbedPane.getTitleAt(selectedTab);
        String utilisateur = titre.substring(2);
        out.println("/msg " + utilisateur + " " + message);
      }
      messageField.setText("");
    }
  }

  private void rafraichirListeUtilisateurs() {
    if (isConnected) {
      out.println("/list");
      // Mettre à jour le combo après avoir demandé la liste
      SwingUtilities.invokeLater(() -> {
        try {
          Thread.sleep(500); // Attendre que la réponse arrive
          mettreAJourComboUtilisateurs();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
    }
  }

  private void envoyerMessagePrive() {
    String selectedUser = userList.getSelectedValue();
    if (selectedUser != null) {
      // Extraire le nom sans le préfixe de statut
      String nomUtilisateur = selectedUser.replace("Connecte : ", "").replace("Hors ligne : ", "")
          .replace("🔴 ", "").replace(" (hors ligne)", "");

      String message = JOptionPane.showInputDialog(this,
          "Message pour " + nomUtilisateur + ":",
          "Message Privé",
          JOptionPane.QUESTION_MESSAGE);

      if (message != null && !message.trim().isEmpty()) {
        creerOngletConversation(nomUtilisateur);
        out.println("/msg " + nomUtilisateur + " " + message);

        for (int i = 1; i < tabbedPane.getTabCount(); i++) {
          if (tabbedPane.getTitleAt(i).contains(nomUtilisateur)) {
            tabbedPane.setSelectedIndex(i);
            break;
          }
        }
      }
    } else {
      JOptionPane.showMessageDialog(this, "Veuillez sélectionner un utilisateur");
    }
  }

  /**
   * Envoie un message hors ligne à un utilisateur (connecté ou déconnecté)
   */
  private void envoyerMessageHorsLigne() {
    // Boîte de dialogue moderne pour saisir le destinataire
    JDialog dialog = new JDialog(this, "📩 Message Hors Ligne", true);
    dialog.setSize(500, 300);
    dialog.setLocationRelativeTo(this);

    JPanel content = new JPanel(new BorderLayout(15, 15));
    content.setBorder(new EmptyBorder(20, 20, 20, 20));
    content.setBackground(Color.WHITE);

    // Titre
    JLabel title = new JLabel("📩 Envoyer un message hors ligne", SwingConstants.CENTER);
    title.setFont(new Font("Segoe UI", Font.BOLD, 16));
    title.setForeground(WARNING_COLOR);

    // Panel de saisie amélioré
    JPanel inputPanel = new JPanel(new GridBagLayout());
    inputPanel.setBackground(Color.WHITE);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(8, 8, 8, 8);
    gbc.anchor = GridBagConstraints.WEST;

    // Destinataire avec liste déroulante
    gbc.gridx = 0;
    gbc.gridy = 0;
    JLabel labelUser = new JLabel("Destinataire :");
    labelUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
    inputPanel.add(labelUser, gbc);

    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;

    // Créer la liste déroulante avec tous les utilisateurs connus
    DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<>();
    comboModel.addElement("🔽 Choisir un utilisateur...");

    // Ajouter les utilisateurs connectés
    for (int i = 0; i < userListModel.getSize(); i++) {
      String user = userListModel.getElementAt(i);
      String nomUser = user.replace("Connecte : ", "").replace("Hors ligne : ", "");
      if (!nomUser.equals(utilisateurActuel)) {
        if (user.startsWith("Connecte : ")) {
          comboModel.addElement("Connecte : " + nomUser);
        } else {
          comboModel.addElement("Hors ligne : " + nomUser);
        }
      }
    }

    // Ajouter tous les utilisateurs du fichier
    for (Utilisateur user : utilisateurs) {
      String nomUser = user.getNom();
      if (!nomUser.equals(utilisateurActuel)) {
        boolean dejaAjoute = false;
        for (int i = 0; i < comboModel.getSize(); i++) {
          String item = comboModel.getElementAt(i);
          if (item.contains(nomUser)) {
            dejaAjoute = true;
            break;
          }
        }
        if (!dejaAjoute) {
          comboModel.addElement("🔴 " + nomUser + " (hors ligne)");
        }
      }
    }

    // Ajouter option de saisie manuelle
    comboModel.addElement("✏️ Saisir un autre nom...");

    JComboBox<String> userCombo = new JComboBox<>(comboModel);
    userCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    userCombo.setBackground(Color.WHITE);
    inputPanel.add(userCombo, gbc);

    // Champ de saisie manuelle (initialement caché)
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.weightx = 0;
    JLabel labelManuel = new JLabel("Nom manuel :");
    labelManuel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
    labelManuel.setForeground(Color.GRAY);
    labelManuel.setVisible(false);
    inputPanel.add(labelManuel, gbc);

    gbc.gridx = 1;
    gbc.weightx = 1.0;
    JTextField userField = new JTextField();
    userField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    userField.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
        new EmptyBorder(5, 8, 5, 8)));
    userField.setVisible(false);
    inputPanel.add(userField, gbc);

    // Gestion du changement de sélection
    userCombo.addActionListener(e -> {
      String selected = (String) userCombo.getSelectedItem();
      boolean isManual = "✏️ Saisir un autre nom...".equals(selected);
      labelManuel.setVisible(isManual);
      userField.setVisible(isManual);

      if (isManual) {
        userField.requestFocus();
      }

      dialog.revalidate();
      dialog.repaint();
    });

    // Message
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.weightx = 0;
    JLabel labelMsg = new JLabel("Message :");
    labelMsg.setFont(new Font("Segoe UI", Font.BOLD, 12));
    inputPanel.add(labelMsg, gbc);

    gbc.gridx = 1;
    gbc.weightx = 1.0;
    JTextField messageField = new JTextField();
    messageField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    messageField.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
        new EmptyBorder(5, 8, 5, 8)));
    inputPanel.add(messageField, gbc);

    // Info supplémentaire
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 2;
    JLabel infoLabel = new JLabel("💡 Le message sera livré immédiatement si connecté, sinon à la reconnexion");
    infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 10));
    infoLabel.setForeground(new Color(108, 117, 125));
    inputPanel.add(infoLabel, gbc);

    // Boutons
    JPanel buttonPanel = new JPanel(new FlowLayout());
    buttonPanel.setBackground(Color.WHITE);

    JButton sendBtn = createModernButton("📤 Envoyer", SUCCESS_COLOR);
    JButton cancelBtn = createModernButton("❌ Annuler", ERROR_COLOR);

    sendBtn.addActionListener(e -> {
      // Récupérer le destinataire selon le mode sélectionné
      String destinataire = "";
      String selectedCombo = (String) userCombo.getSelectedItem();

      if ("✏️ Saisir un autre nom...".equals(selectedCombo)) {
        // Mode saisie manuelle
        destinataire = userField.getText().trim();
      } else if (!"🔽 Choisir un utilisateur...".equals(selectedCombo)) {
        // Mode sélection depuis liste
        destinataire = selectedCombo.replace("Connecte : ", "").replace("Hors ligne : ", "")
            .replace("🔴 ", "").replace(" (hors ligne)", "");
      }

      String message = messageField.getText().trim();

      if (destinataire.isEmpty() || message.isEmpty()) {
        JOptionPane.showMessageDialog(dialog,
            "Veuillez choisir un destinataire et saisir un message",
            "Champs manquants",
            JOptionPane.WARNING_MESSAGE);
        return;
      }

      // Envoyer le message (le serveur gère automatiquement le hors ligne)
      if (isConnected) {
        creerOngletConversation(destinataire);
        out.println("/msg " + destinataire + " " + message);

        // Confirmation visuelle moderne
        String statusMessage = selectedCombo != null && selectedCombo.startsWith("Connecte :")
            ? "Message envoyé à " + destinataire + " (connecté)"
            : "Message envoyé à " + destinataire + "\n(Il le recevra à sa prochaine connexion)";

        JOptionPane.showMessageDialog(dialog,
            statusMessage,
            "Message envoyé",
            JOptionPane.INFORMATION_MESSAGE);

        dialog.dispose();
      } else {
        JOptionPane.showMessageDialog(dialog,
            "Vous devez être connecté pour envoyer des messages",
            "Non connecté",
            JOptionPane.ERROR_MESSAGE);
      }
    });

    cancelBtn.addActionListener(e -> dialog.dispose());

    buttonPanel.add(sendBtn);
    buttonPanel.add(cancelBtn);

    content.add(title, BorderLayout.NORTH);
    content.add(inputPanel, BorderLayout.CENTER);
    content.add(buttonPanel, BorderLayout.SOUTH);

    dialog.add(content);
    dialog.setVisible(true);
  }

  /**
   * Déconnecte l'utilisateur du serveur
   */
  private void deconnecter() {
    if (isConnected) {
      int confirmation = JOptionPane.showConfirmDialog(
          this,
          "Êtes-vous sûr de vouloir vous déconnecter ?",
          "Confirmation de déconnexion",
          JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE);

      if (confirmation == JOptionPane.YES_OPTION) {
        try {
          if (out != null) {
            out.println("/bye");
          }
          if (socket != null && !socket.isClosed()) {
            socket.close();
          }
          isConnected = false;
          statusLabel.setText("Deconnecte");
          statusLabel.setForeground(ERROR_COLOR);
          userInfoLabel.setText("Deconnecte");
          messageField.setEnabled(false);
          sendButton.setEnabled(false);
          userListModel.clear();

          // Afficher message de confirmation
          JOptionPane.showMessageDialog(this,
              "Vous avez été déconnecté avec succès.",
              "Déconnexion réussie",
              JOptionPane.INFORMATION_MESSAGE);

          // Possibilité de se reconnecter
          int reconnecter = JOptionPane.showConfirmDialog(
              this,
              "Voulez-vous vous reconnecter ?",
              "Reconnexion",
              JOptionPane.YES_NO_OPTION);

          if (reconnecter == JOptionPane.YES_OPTION) {
            connecterAuServeur();
          }
        } catch (IOException e) {
          JOptionPane.showMessageDialog(this,
              "Erreur lors de la déconnexion : " + e.getMessage(),
              "Erreur",
              JOptionPane.ERROR_MESSAGE);
        }
      }
    } else {
      JOptionPane.showMessageDialog(this,
          "Vous n'êtes pas connecté.",
          "Pas de connexion",
          JOptionPane.WARNING_MESSAGE);
    }
  }

  /**
   * Affiche tous les utilisateurs (connectés + connus du fichier)
   */
  private void afficherTousUtilisateurs() {
    userListModel.clear();

    // Ajouter les utilisateurs connectés avec indicateur vert
    if (isConnected) {
      out.println("/list");
    }

    // Ajouter les utilisateurs connus du fichier avec indicateur rouge
    // (déconnectés)
    for (Utilisateur user : utilisateurs) {
      String nomUser = user.getNom();
      boolean dejaAjoute = false;

      // Vérifier s'il n'est pas déjà dans la liste (connecté)
      for (int i = 0; i < userListModel.getSize(); i++) {
        String item = userListModel.getElementAt(i);
        if (item.contains(nomUser)) {
          dejaAjoute = true;
          break;
        }
      }

      // S'il n'est pas connecté, l'ajouter comme déconnecté
      if (!dejaAjoute) {
        userListModel.addElement("🔴 " + nomUser + " (hors ligne)");
      }
    }

    // Message informatif
    statusLabel.setText("💡 🟢 = Connecté, 🔴 = Hors ligne (recevra le message à la reconnexion)");
    statusLabel.setForeground(new Color(108, 117, 125));
  }

  // Méthodes pour la gestion des groupes

  private void afficherInfoGroupe(String nomGroupe) {
    if (out != null) {
      out.println("/groupe-membres " + nomGroupe);
    }
  }

  private void rafraichirListeGroupes() {
    if (out != null) {
      out.println("/groupe-liste");
      // Également charger la liste des utilisateurs pour le combobox
      out.println("/list");
    }
  }

  private void voirMesGroupes() {
    if (out != null) {
      out.println("/mes-groupes");
      // Également charger la liste des utilisateurs pour le combobox
      out.println("/list");
    }
  }

  private void creerNouveauGroupe() {
    String nom = groupNameField.getText().trim();
    String description = groupDescriptionField.getText().trim();

    if (nom.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Veuillez entrer un nom pour le groupe", "Erreur", JOptionPane.ERROR_MESSAGE);
      return;
    }

    if (description.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Veuillez entrer une description pour le groupe", "Erreur",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    if (out != null) {
      out.println("/groupe-creer " + nom + " " + description);
      groupNameField.setText("");
      groupDescriptionField.setText("");

      // Actualiser automatiquement la liste des groupes après création
      SwingUtilities.invokeLater(() -> {
        try {
          Thread.sleep(500); // Attendre un peu que le serveur traite la création
          rafraichirListeGroupes();
          voirMesGroupes();
          mettreAJourComboUtilisateurs(); // Mettre à jour le combo des utilisateurs
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
    }
  }

  private void ajouterMembreGroupe() {
    String selectedGroup = groupList.getSelectedValue();
    String selectedUser = (String) groupMemberCombo.getSelectedItem();

    if (selectedGroup == null) {
      JOptionPane.showMessageDialog(this, "Veuillez sélectionner un groupe", "Erreur", JOptionPane.ERROR_MESSAGE);
      return;
    }

    if (selectedUser == null || selectedUser.startsWith("🔽")) {
      JOptionPane.showMessageDialog(this, "Veuillez sélectionner un utilisateur", "Erreur",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    String utilisateur = "";

    // Gestion de la saisie manuelle
    if (selectedUser.startsWith("✏️")) {
      utilisateur = JOptionPane.showInputDialog(this,
          "Entrez le nom d'utilisateur à ajouter au groupe :",
          "Saisir nom d'utilisateur",
          JOptionPane.QUESTION_MESSAGE);

      if (utilisateur == null || utilisateur.trim().isEmpty()) {
        JOptionPane.showMessageDialog(this, "Nom d'utilisateur invalide", "Erreur", JOptionPane.ERROR_MESSAGE);
        return;
      }
      utilisateur = utilisateur.trim();
    } else {
      // Extraire le nom d'utilisateur des différents formats
      utilisateur = selectedUser
          .replace("Connecté : ", "")
          .replace("Connecte : ", "")
          .replace("Hors ligne : ", "")
          .replace("💤 ", "")
          .replace(" (déconnecté)", "")
          .trim();
    }

    if (out != null) {
      out.println("/groupe-ajouter " + selectedGroup + " " + utilisateur);
      groupMemberCombo.setSelectedIndex(0); // Reset to default
      // Actualiser les informations du groupe après ajout
      SwingUtilities.invokeLater(() -> afficherInfoGroupe(selectedGroup));
    }
  }

  private void supprimerMembreGroupe() {
    String selectedGroup = groupList.getSelectedValue();
    String selectedUser = (String) groupMemberCombo.getSelectedItem();

    if (selectedGroup == null) {
      JOptionPane.showMessageDialog(this, "Veuillez sélectionner un groupe", "Erreur", JOptionPane.ERROR_MESSAGE);
      return;
    }

    if (selectedUser == null || selectedUser.startsWith("🔽")) {
      JOptionPane.showMessageDialog(this, "Veuillez sélectionner un utilisateur", "Erreur",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    String utilisateur = "";

    // Gestion de la saisie manuelle
    if (selectedUser.startsWith("✏️")) {
      utilisateur = JOptionPane.showInputDialog(this,
          "Entrez le nom d'utilisateur à supprimer du groupe :",
          "Saisir nom d'utilisateur",
          JOptionPane.QUESTION_MESSAGE);

      if (utilisateur == null || utilisateur.trim().isEmpty()) {
        JOptionPane.showMessageDialog(this, "Nom d'utilisateur invalide", "Erreur", JOptionPane.ERROR_MESSAGE);
        return;
      }
      utilisateur = utilisateur.trim();
    } else {
      // Extraire le nom d'utilisateur des différents formats
      utilisateur = selectedUser
          .replace("Connecté : ", "")
          .replace("Connecte : ", "")
          .replace("Hors ligne : ", "")
          .replace("💤 ", "")
          .replace(" (déconnecté)", "")
          .trim();
    }

    if (out != null) {
      out.println("/groupe-supprimer " + selectedGroup + " " + utilisateur);
      groupMemberCombo.setSelectedIndex(0); // Reset to default
      // Actualiser les informations du groupe après suppression
      SwingUtilities.invokeLater(() -> afficherInfoGroupe(selectedGroup));
    }
  }

  private void mettreAJourListeGroupes(String message) {
    SwingUtilities.invokeLater(() -> {
      groupListModel.clear();

      // Parse le message pour extraire les groupes
      String[] lines = message.split("\n");
      for (String line : lines) {
        line = line.trim();
        if (line.startsWith("- ")) {
          // Format: "- NomGroupe (X membres) - Description"
          String groupeName = line.substring(2);
          if (groupeName.contains(" (")) {
            groupeName = groupeName.substring(0, groupeName.indexOf(" ("));
          }
          if (groupeName.contains(" - ")) {
            groupeName = groupeName.substring(0, groupeName.indexOf(" - "));
          }
          groupListModel.addElement(groupeName.trim());
        } else if (line.contains("(Modérateur)") || line.contains("(Membre)")) {
          // Format des groupes de l'utilisateur: "- NomGroupe (Role) - Description"
          String groupeName = line.substring(2);
          if (groupeName.contains(" (")) {
            groupeName = groupeName.substring(0, groupeName.indexOf(" ("));
          }
          groupListModel.addElement(groupeName.trim());
        }
      }

      // Si aucun groupe trouvé, vérifier si c'est un message d'erreur
      if (groupListModel.isEmpty()) {
        if (message.contains("Aucun groupe") || message.contains("Vous n'êtes membre d'aucun groupe")) {
          groupInfoArea.setText("Aucun groupe trouvé.\nCréez votre premier groupe ci-dessous !");
        }
      }
    });
  }

  private void mettreAJourInfoGroupe(String message) {
    SwingUtilities.invokeLater(() -> {
      // Améliorer l'affichage des informations du groupe
      String formattedMessage = message.replace("=== MEMBRES DU GROUPE", "📋 MEMBRES DU GROUPE")
          .replace("===", "")
          .replace("Créateur :", "👑 Créateur :")
          .replace("Modérateurs :", "🛡️ Modérateurs :")
          .replace("Membres :", "👥 Membres :")
          .replace("Total :", "📊 Total :");
      groupInfoArea.setText(formattedMessage);
    });
  }

  private void traiterLigneGroupe(String ligne) {
    SwingUtilities.invokeLater(() -> {
      String ligneTrimmee = ligne.trim();
      if (ligneTrimmee.startsWith("- ")) {
        // Format: "- NomGroupe (X membres) - Description" ou "- NomGroupe (Role) -
        // Description"
        String groupeName = ligneTrimmee.substring(2);
        if (groupeName.contains(" (")) {
          groupeName = groupeName.substring(0, groupeName.indexOf(" ("));
        }
        if (groupeName.contains(" - ")) {
          groupeName = groupeName.substring(0, groupeName.indexOf(" - "));
        }
        groupeName = groupeName.trim();

        if (!groupeName.isEmpty()) {
          // Vérifier si le groupe n'est pas déjà dans la liste
          boolean dejaDansListe = false;
          for (int i = 0; i < groupListModel.getSize(); i++) {
            if (groupListModel.getElementAt(i).equals(groupeName)) {
              dejaDansListe = true;
              break;
            }
          }

          if (!dejaDansListe) {
            System.out.println("DEBUG - Ajout du groupe à la liste: " + groupeName);
            groupListModel.addElement(groupeName);
          }
        }
      }
    });
  }

  private void mettreAJourComboUtilisateurs() {
    if (groupMemberCombo != null) {
      SwingUtilities.invokeLater(() -> {
        String selectedItem = (String) groupMemberCombo.getSelectedItem();
        groupMemberCombo.removeAllItems();
        groupMemberCombo.addItem("🔽 Sélectionner un utilisateur...");

        // Ajouter les utilisateurs connectés d'abord
        for (int i = 0; i < userListModel.getSize(); i++) {
          String user = userListModel.getElementAt(i);
          if (user.startsWith("Connecte : ") || user.startsWith("Connecté : ")) {
            groupMemberCombo.addItem(user);
          }
        }

        // Puis ajouter les utilisateurs hors ligne affichés
        for (int i = 0; i < userListModel.getSize(); i++) {
          String user = userListModel.getElementAt(i);
          if (user.startsWith("Hors ligne : ")) {
            groupMemberCombo.addItem(user);
          }
        }

        // Ajouter TOUS les utilisateurs connus du fichier (même ceux pas actuellement
        // affichés)
        for (Utilisateur user : utilisateurs) {
          String nomUser = user.getNom();
          if (!nomUser.equals(utilisateurActuel)) { // Ne pas s'inclure soi-même
            boolean dejaAjoute = false;
            // Vérifier si l'utilisateur n'est pas déjà dans le combo
            for (int i = 0; i < groupMemberCombo.getItemCount(); i++) {
              String item = groupMemberCombo.getItemAt(i);
              if (item.contains(nomUser)) {
                dejaAjoute = true;
                break;
              }
            }
            if (!dejaAjoute) {
              groupMemberCombo.addItem("💤 " + nomUser + " (déconnecté)");
            }
          }
        }

        // Ajouter une option pour saisir manuellement un nom
        groupMemberCombo.addItem("✏️ Saisir un autre nom...");

        System.out.println("DEBUG - Combo mis à jour avec " + (groupMemberCombo.getItemCount() - 1) + " utilisateurs");

        // Restaurer la sélection si possible
        if (selectedItem != null && !selectedItem.startsWith("🔽")) {
          groupMemberCombo.setSelectedItem(selectedItem);
        }
      });
    }
  }
}