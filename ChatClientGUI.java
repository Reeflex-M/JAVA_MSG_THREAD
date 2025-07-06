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
 * Interface graphique moderne pour le client de chat multi-utilisateurs
 * Utilise un design moderne avec couleurs Material Design et thèmes
 */
public class ChatClientGUI extends JFrame {
  // Couleurs modernes Material Design
  private static final Color PRIMARY_COLOR = new Color(33, 150, 243);
  private static final Color SECONDARY_COLOR = new Color(63, 81, 181);
  private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
  private static final Color WARNING_COLOR = new Color(255, 152, 0);
  private static final Color ERROR_COLOR = new Color(244, 67, 54);
  private static final Color BACKGROUND_COLOR = new Color(250, 250, 250);

  // Composants principaux
  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  private String utilisateurActuel;
  private ArrayList<Utilisateur> utilisateurs;

  // Interface moderne
  private JTabbedPane tabbedPane;
  private JTextArea chatGeneralArea;
  private JTextField messageField;
  private JButton sendButton;
  private JList<String> userList;
  private DefaultListModel<String> userListModel;
  private JLabel statusLabel;

  // Variables d'état
  private boolean isConnected = false;
  private Map<String, JTextArea> conversationsPrivees = new HashMap<>();

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      try {
        // Essayer le thème système
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
    super("💬 Chat Multi-Utilisateurs - Interface Moderne");
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
      JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage());
      dispose();
    }
  }

  private void initializeModernUI() {
    setLayout(new BorderLayout(10, 10));

    // Panel principal moderne
    JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
    mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
    mainPanel.setBackground(BACKGROUND_COLOR);

    // Header moderne avec gradient
    JPanel header = createModernHeader();
    mainPanel.add(header, BorderLayout.NORTH);

    // Contenu principal
    JPanel content = new JPanel(new BorderLayout(15, 0));
    content.setBackground(BACKGROUND_COLOR);

    // Zone de chat avec onglets
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
    content.add(tabbedPane, BorderLayout.CENTER);

    // Panel utilisateurs moderne
    JPanel userPanel = createModernUserPanel();
    content.add(userPanel, BorderLayout.EAST);

    mainPanel.add(content, BorderLayout.CENTER);

    // Footer avec champ de saisie
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

    JLabel title = new JLabel("💬 Chat Multi-Utilisateurs Moderne");
    title.setFont(new Font("Segoe UI", Font.BOLD, 22));
    title.setForeground(Color.WHITE);

    JButton aboutBtn = createModernButton("ℹ️ À propos", new Color(255, 255, 255, 50));
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
        "👥 Utilisateurs (🟢 Connectés)",
        TitledBorder.CENTER, TitledBorder.TOP,
        new Font("Segoe UI", Font.BOLD, 12), SUCCESS_COLOR));

    // Liste des utilisateurs avec indicateurs de statut
    userListModel = new DefaultListModel<>();
    userList = new JList<>(userListModel);
    userList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    userList.setBackground(new Color(248, 250, 252));
    userList.setBorder(new EmptyBorder(10, 10, 10, 10));

    // Renderer personnalisé pour afficher le statut
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
    userScroll.setBorder(null);

    // Panel de boutons étendu
    JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 5, 5));
    buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
    buttonPanel.setBackground(Color.WHITE);

    JButton refreshBtn = createModernButton("🔄 Actualiser", PRIMARY_COLOR);
    JButton messageBtn = createModernButton("💌 Message Privé", SECONDARY_COLOR);
    JButton offlineBtn = createModernButton("📩 Message Hors Ligne", WARNING_COLOR);
    JButton showAllBtn = createModernButton("👁️ Voir Tous", new Color(156, 39, 176));

    refreshBtn.addActionListener(e -> rafraichirListeUtilisateurs());
    messageBtn.addActionListener(e -> envoyerMessagePrive());
    offlineBtn.addActionListener(e -> envoyerMessageHorsLigne());
    showAllBtn.addActionListener(e -> afficherTousUtilisateurs());

    buttonPanel.add(refreshBtn);
    buttonPanel.add(messageBtn);
    buttonPanel.add(offlineBtn);
    buttonPanel.add(showAllBtn);

    panel.add(userScroll, BorderLayout.CENTER);
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

    sendButton = createModernButton("📤 Envoyer", SUCCESS_COLOR);
    sendButton.setPreferredSize(new Dimension(120, 45));
    sendButton.addActionListener(e -> envoyerMessage());

    statusLabel = new JLabel("💡 Connectez-vous pour commencer");
    statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
    statusLabel.setForeground(new Color(108, 117, 125));

    footer.add(messageField, BorderLayout.CENTER);
    footer.add(sendButton, BorderLayout.EAST);
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
    if (Files.exists(Paths.get("utilisateurs.txt"))) {
      for (String ligne : Files.readAllLines(Paths.get("utilisateurs.txt"))) {
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
    ArrayList<String> lignes = new ArrayList<>();
    for (Utilisateur u : utilisateurs) {
      lignes.add(u.getNom() + "," + (u.getAdresseIP() != null ? u.getAdresseIP() : "localhost"));
    }
    Files.write(Paths.get("utilisateurs.txt"), lignes);
  }

  private void connecterAuServeur() {
    try {
      socket = new Socket("localhost", 1234);
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      String serverPrompt = in.readLine();
      out.println(utilisateurActuel);

      isConnected = true;
      statusLabel.setText("✅ Connecté : " + utilisateurActuel);
      statusLabel.setForeground(SUCCESS_COLOR);
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
            statusLabel.setText("❌ Connexion perdue");
            statusLabel.setForeground(ERROR_COLOR);
            isConnected = false;
          });
        }
      }).start();

    } catch (IOException e) {
      JOptionPane.showMessageDialog(this, "Erreur connexion : " + e.getMessage());
      statusLabel.setText("❌ Échec connexion");
      statusLabel.setForeground(ERROR_COLOR);
    }
  }

  private void traiterMessage(String message) {
    if (message.equals("Entrez votre nom d'utilisateur :"))
      return;

    if (message.startsWith("[PRIVÉ]")) {
      traiterMessagePrive(message);
    } else if (message.startsWith("Utilisateurs connectés")) {
      mettreAJourListeUtilisateurs(message);
    } else {
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
    userListModel.clear();
    if (listeMessage.startsWith("Utilisateurs connectés : ")) {
      String users = listeMessage.substring("Utilisateurs connectés : ".length());
      String[] userArray = users.split(", ");
      for (String user : userArray) {
        if (!user.trim().isEmpty()) {
          // Ajouter avec indicateur vert pour les connectés
          userListModel.addElement("🟢 " + user.trim());
        }
      }
    } else if (listeMessage.equals("Aucun utilisateur connecté")) {
      // Si aucun utilisateur connecté, afficher quand même les utilisateurs connus
      // comme déconnectés
      for (Utilisateur user : utilisateurs) {
        if (!user.getNom().equals(utilisateurActuel)) { // Ne pas s'afficher soi-même
          userListModel.addElement("🔴 " + user.getNom() + " (hors ligne)");
        }
      }
    }

    // Mettre à jour le statut
    int connectes = 0;
    for (int i = 0; i < userListModel.getSize(); i++) {
      if (userListModel.getElementAt(i).startsWith("🟢 ")) {
        connectes++;
      }
    }

    statusLabel.setText("👥 " + connectes + " connecté(s) • Cliquez 'Voir Tous' pour les utilisateurs hors ligne");
  }

  private void ajouterMessageGeneral(String message) {
    chatGeneralArea.append(message + "\n");
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
      area.append(message + "\n");
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
    }
  }

  private void envoyerMessagePrive() {
    String selectedUser = userList.getSelectedValue();
    if (selectedUser != null) {
      // Extraire le nom sans le préfixe de statut
      String nomUtilisateur = selectedUser.replace("🟢 ", "").replace("🔴 ", "");

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
      String nomUser = user.replace("🟢 ", "").replace("🔴 ", "").replace(" (hors ligne)", "");
      if (!nomUser.equals(utilisateurActuel)) {
        if (user.startsWith("🟢")) {
          comboModel.addElement("🟢 " + nomUser + " (connecté)");
        } else {
          comboModel.addElement("🔴 " + nomUser + " (hors ligne)");
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
        destinataire = selectedCombo.replace("🟢 ", "").replace("🔴 ", "")
            .replace(" (connecté)", "").replace(" (hors ligne)", "");
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
        String statusMessage = selectedCombo != null && selectedCombo.startsWith("🟢")
            ? "Message envoyé à " + destinataire + " (connecté) ✅"
            : "Message envoyé à " + destinataire + "\n(Il le recevra à sa prochaine connexion) 📩";

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
}