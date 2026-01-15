package com.mycompany.photographytagger;

import DBConnect.DBConnect;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.Component; 
import java.awt.Font;      
import java.awt.Color;     
import javax.swing.JLabel; 
import javax.swing.SwingConstants; 
import javax.swing.border.EmptyBorder; 
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
public class page extends javax.swing.JFrame {
    private boolean isGridView = true; 
    private JButton fab; 
private java.util.Map<String, ImageIcon> imageCache = new java.util.HashMap<>();
private JScrollPane scrollPane;
private int selectedPhotoId = -1; 
    private java.util.ArrayList<java.io.File> uploadedFiles = new java.util.ArrayList<>();
    private javax.swing.JTextField jTextFieldTag;
    
    
    private javax.swing.JComboBox<String> tagCombo = new javax.swing.JComboBox<>();

    
    private final java.awt.Color SIDEBAR_COLOR = new java.awt.Color(33, 37, 41);
    private final java.awt.Color SIDEBAR_HOVER = new java.awt.Color(52, 58, 64);
    private final java.awt.Color BACKGROUND_COLOR = new java.awt.Color(240, 242, 245);
    private final java.awt.Color ACCENT_BLUE = new java.awt.Color(13, 110, 253);
    public page() {
        initComponents();
        applyUI();
        loadPhotoGallery();
        setupTagFilter();
        setLocationRelativeTo(null);
        
    }
private void setupTagFilter() {
    try {
        // পুরনো লিসেনার রিমুভ করা যাতে বারবার একই কাজ না হয়
        for (ActionListener al : tagCombo.getActionListeners()) {
            tagCombo.removeActionListener(al);
        }

        tagCombo.removeAllItems();
        String allPhotosText = "All Photos"; 
        tagCombo.addItem(allPhotosText);
        
        Connection con = DBConnect.getConnection();
        // ডাটাবেস থেকে ইউনিক ট্যাগগুলো নিয়ে আসা
        String sql = "SELECT DISTINCT tag FROM photo_app " +
                     "WHERE tag IS NOT NULL AND tag != '' " +
                     "AND tag NOT IN ('Tag', 'Favorite', 'General', 'favourite', 'general')";
        
        PreparedStatement pst = con.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            tagCombo.addItem(rs.getString("tag"));
        }
        con.close();

        // কম্বো বক্স ডিজাইন (Basic UI)
        tagCombo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = super.createArrowButton();
                button.setBackground(new Color(75, 75, 75)); 
                button.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
                return button;
            }
        });

        // সিলেকশন লজিক আপডেট
        tagCombo.addActionListener(e -> {
            Object selectedItem = tagCombo.getSelectedItem();
            if (selectedItem == null) return;
            
            String selected = selectedItem.toString();
            if (selected.equals(allPhotosText)) {
                // All Photos সিলেক্ট করলে মেইন গ্যালারি লোড হবে
                loadPhotoGallery(); 
            } else {
                // নির্দিষ্ট ট্যাগ সিলেক্ট করলে ফিল্টার হবে
                filterByTag(selected); 
            }
        });

    } catch (Exception e) { 
        e.printStackTrace(); 
    }

    // কম্বো বক্সের কালার এবং বর্ডার
    tagCombo.setBackground(new Color(45, 52, 54));
    tagCombo.setForeground(Color.WHITE);
    tagCombo.setBorder(BorderFactory.createLineBorder(new Color(120, 120, 120))); 
    
    if (jPanel2 != null) {
        jPanel2.revalidate();
        jPanel2.repaint();
    }
}
   // নির্দিষ্ট ট্যাগ অনুযায়ী ছবি ফিল্টার করার মেথড
private void filterByTag(String tagName) {
    if (mainContent == null) return;
    
    mainContent.removeAll();
    int gap = 20; 
    int currentWidth = (scrollPane != null && scrollPane.getWidth() > 0) ? scrollPane.getWidth() : 900;
    
    // বর্তমান ভিউ মোড (Grid/List) অনুযায়ী লেআউট সেট করা
    if (isGridView) {
        mainContent.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, gap, gap));
    } else {
        mainContent.setLayout(new javax.swing.BoxLayout(mainContent, javax.swing.BoxLayout.Y_AXIS));
    }

    try (java.sql.Connection con = DBConnect.getConnection()) {
        // ডাটাবেস থেকে শুধু ওই ট্যাগ এর ছবিগুলো নিয়ে আসা
        String sql = "SELECT * FROM photo_app WHERE tag = ? AND file_path IS NOT NULL AND file_path != '' ORDER BY id DESC";
        java.sql.PreparedStatement pst = con.prepareStatement(sql);
        pst.setString(1, tagName);
        java.sql.ResultSet rs = pst.executeQuery();

        int photoCount = 0;
        while (rs.next()) {
            final String path = rs.getString("file_path");
            final String tag = rs.getString("tag");
            final int id = rs.getInt("id");

            java.io.File imgFile = new java.io.File(path);
            if (!imgFile.exists()) continue; 

            photoCount++; 

            if (isGridView) {
                // --- GRID VIEW RENDERING ---
                int cardWidth = (currentWidth - (gap * 5)) / 3; 
                JPanel card = new JPanel(new BorderLayout());
                card.setPreferredSize(new Dimension(cardWidth, cardWidth));
                card.setBackground(new Color(33, 34, 35));

                ImageIcon displayIcon = getScaledIcon(path, cardWidth, cardWidth);
                JLabel imgLabel = new JLabel(displayIcon);
                imgLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                imgLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        selectedPhotoId = id; showFullImage(path);
                    }
                });
                card.add(imgLabel, BorderLayout.CENTER);
                mainContent.add(card);
            } else {
                // --- LIST VIEW RENDERING ---
                JPanel row = new JPanel(new BorderLayout(15, 0));
                row.setMaximumSize(new Dimension(currentWidth - 40, 100));
                row.setPreferredSize(new Dimension(currentWidth - 40, 100));
                row.setBackground(new Color(33, 34, 35));
                row.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

                ImageIcon thumb = getScaledIcon(path, 80, 80);
                JLabel imgLabel = new JLabel(thumb);
                
                JPanel info = new JPanel(new java.awt.GridLayout(2, 1));
                info.setOpaque(false);
                JLabel nameLbl = new JLabel("Photo ID: #" + id);
                nameLbl.setForeground(Color.WHITE);
                JLabel tagLbl = new JLabel("Tag: " + tag);
                tagLbl.setForeground(new Color(150, 150, 150));
                
                info.add(nameLbl);
                info.add(tagLbl);

                row.add(imgLabel, BorderLayout.WEST);
                row.add(info, BorderLayout.CENTER);
                row.setCursor(new Cursor(Cursor.HAND_CURSOR));
                row.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        selectedPhotoId = id; showFullImage(path);
                    }
                });
                mainContent.add(row);
                mainContent.add(Box.createVerticalStrut(5));
            }
        }

        // স্ক্রল এরিয়া ঠিক করা
        if (isGridView) {
            int rows = (int) Math.ceil((double) photoCount / 3);
            int itemH = (currentWidth - (gap * 5)) / 3;
            mainContent.setPreferredSize(new Dimension(currentWidth - 50, rows * (itemH + gap) + 50));
        } else {
            mainContent.setPreferredSize(new Dimension(currentWidth - 50, photoCount * 110 + 50));
        }

    } catch (Exception e) { 
        e.printStackTrace();
    }

    mainContent.revalidate();
    mainContent.repaint();
}

// আপনার এরর হওয়া মেথডটি নিচে দেওয়া হলো (এটি যোগ না করলে 'thumb' লাইনে এরর আসবে)
private ImageIcon getScaledIcon(String path, int w, int h) {
    try {
        String key = path + "_" + w;
        // imageCache থাকলে চেক করবে
        if (imageCache != null && imageCache.containsKey(key)) {
            return imageCache.get(key);
        }
        
        ImageIcon icon = new ImageIcon(path);
        Image img = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        ImageIcon scaled = new ImageIcon(img);
        
        if(imageCache != null) imageCache.put(key, scaled);
        return scaled;
    } catch (Exception e) {
        return new ImageIcon(); // কোনো সমস্যা হলে খালি আইকন রিটার্ন করবে
    }
}
    private void renderImageThumb(String path, int id) {
        ImageIcon icon = new ImageIcon(new ImageIcon(path).getImage().getScaledInstance(180, 150, Image.SCALE_SMOOTH));
        JLabel label = new JLabel(icon);
        label.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                selectedPhotoId = id;
                showFullImage(path);
            }
        });
        mainContent.add(label);
    }
    private JPanel mainContent; 

  private void applyUI() {
    // কালার প্যালেট
    Color SIDEBAR_COLOR = new Color(30, 31, 32); 
    Color HEADER_COLOR = new Color(24, 25, 26);
    Color MAIN_BG = new Color(18, 18, 18);

    // মেইন উইন্ডো সেটআপ
    this.getContentPane().removeAll();
    this.getContentPane().setLayout(new BorderLayout());

    // --- হেডার সেকশন (jPanel1) ---
    jPanel1.setBackground(HEADER_COLOR);
    jPanel1.setPreferredSize(new Dimension(0, 80));
    jPanel1.removeAll(); 
    jPanel1.setLayout(null);

    int currentWidth = this.getWidth() > 0 ? this.getWidth() : 1100;
    int midX = currentWidth / 2;

    jLabel1.setText("Photograph Organizer & Tagger");
    jLabel1.setFont(new Font("Segoe UI", Font.BOLD, 22));
    jLabel1.setForeground(Color.WHITE);
    jLabel1.setHorizontalAlignment(SwingConstants.CENTER); 
    jLabel1.setBounds(midX - 250, 20, 500, 35); 
    jPanel1.add(jLabel1);

    JLabel welcomeLbl = new JLabel("WELCOME TO YOUR PHOTO GALLERY!");
    welcomeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    welcomeLbl.setForeground(new Color(150, 150, 150)); 
    welcomeLbl.setHorizontalAlignment(SwingConstants.CENTER);
    welcomeLbl.setBounds(midX - 300, 50, 600, 20); 
    jPanel1.add(welcomeLbl);

    if(jButton5 != null) { // Search Button
        jButton5.setBounds(currentWidth - 140, 22, 100, 35);
        jPanel1.add(jButton5);
    }

    // --- সাইডবার সেকশন (jPanel2) ---
    jPanel2.setBackground(SIDEBAR_COLOR);
    jPanel2.setPreferredSize(new Dimension(230, 0));
    jPanel2.setLayout(new BoxLayout(jPanel2, BoxLayout.Y_AXIS));
    jPanel2.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
    jPanel2.removeAll();

    addSidebarLabel(new JLabel("MAIN MENU"));
    
    // Grid View বাটন (Home হিসেবে ব্যবহার করা হচ্ছে)
    if(jButton12 != null) { 
        styleSidebarButton(jButton12); 
        jButton12.setText("🏠Home"); 
        for(ActionListener al : jButton12.getActionListeners()) jButton12.removeActionListener(al);
        jButton12.addActionListener(e -> {
            isGridView = true;
            loadPhotoGallery();
        });
        jPanel2.add(jButton12); 
    }

    addSidebarLabel(new JLabel("LIBRARY"));
    
    // List View বাটন
    if(jButton8 != null) {
        styleSidebarButton(jButton8);
        jButton8.setText("📋 List View");
        for(ActionListener al : jButton8.getActionListeners()) jButton8.removeActionListener(al);
        jButton8.addActionListener(e -> {
            isGridView = false;
            loadPhotoGallery();
        });
        jPanel2.add(jButton8);
    }

    // ট্যাগ ফিল্টার কম্বো বক্স
    if(tagCombo != null) {
        tagCombo.setMaximumSize(new Dimension(210, 40));
        tagCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        tagCombo.setBackground(new Color(45, 45, 45));
        tagCombo.setForeground(Color.WHITE);
        jPanel2.add(Box.createVerticalStrut(10));
        jPanel2.add(tagCombo);
    }

    jPanel2.add(Box.createVerticalGlue());

    // --- সিস্টেম সেকশন ---
    addSidebarLabel(new JLabel("SYSTEM"));
    
    JButton btnSettings = new JButton("⚙ Settings");
    styleSidebarButton(btnSettings);
    btnSettings.addActionListener(e -> {
        String[] options = {"Clear Cache", "Reset Gallery", "App Info", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, "Settings", "Menu", 0, 1, null, options, options[0]);
        if (choice == 0) { 
            if (imageCache != null) imageCache.clear();
            loadPhotoGallery();
        }
    });
    jPanel2.add(btnSettings);

    JButton btnLogout = new JButton("🚪 Logout");
    styleSidebarButton(btnLogout);
    btnLogout.addActionListener(e -> {
        if (JOptionPane.showConfirmDialog(this, "Logout?", "Confirm", 0) == 0) System.exit(0);
    });
    jPanel2.add(btnLogout);

    // --- মেইন কন্টেন্ট এরিয়া ---
    if (mainContent == null) mainContent = new JPanel();
    mainContent.setBackground(MAIN_BG);

    scrollPane = new JScrollPane(mainContent);
    scrollPane.setBorder(null);
    scrollPane.getViewport().setBackground(MAIN_BG);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.getVerticalScrollBar().setUnitIncrement(25);

    // মেইন ফ্রেম লেআউটে কম্পোনেন্টগুলো যোগ করা
    this.add(jPanel1, BorderLayout.NORTH);
    this.add(jPanel2, BorderLayout.WEST);
    this.add(scrollPane, BorderLayout.CENTER);

    setupFAB(); // Floating Action Button (+)

    // উইন্ডো রিসাইজ হলে গ্যালারি অ্যাডজাস্ট করা
    this.addComponentListener(new java.awt.event.ComponentAdapter() {
        @Override
        public void componentResized(java.awt.event.ComponentEvent evt) {
            if (fab != null) {
                fab.setBounds(getWidth() - 90, getHeight() - 130, 60, 60);
            }
            SwingUtilities.invokeLater(() -> loadPhotoGallery());
        }
    });

    // শুরুতে গ্যালারি লোড করা
    SwingUtilities.invokeLater(() -> loadPhotoGallery());
    
    this.revalidate();
    this.repaint();
}
private void setupFAB() {
    
    fab = new JButton("+"); 
    fab.setFont(new Font("Segoe UI", Font.PLAIN, 30));
    fab.setForeground(Color.WHITE);
    fab.setBackground(new Color(45, 136, 255));
    fab.setFocusPainted(false);
    fab.setBorderPainted(false);
    fab.setContentAreaFilled(false);
    fab.setCursor(new Cursor(Cursor.HAND_CURSOR));

   
    fab.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
        @Override
        public void paint(Graphics g, JComponent c) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.getBackground());
            g2.fillOval(0, 0, c.getWidth(), c.getHeight());
            g2.dispose();
            super.paint(g, c);
        }
    });

    
    this.getLayeredPane().add(fab, JLayeredPane.PALETTE_LAYER);
    
    
    this.addComponentListener(new java.awt.event.ComponentAdapter() {
        @Override
        public void componentResized(java.awt.event.ComponentEvent e) {
            if (fab != null) {
                
                fab.setBounds(getWidth() - 90, getHeight() - 130, 60, 60);
            }
        }
    });
    
   
    fab.addActionListener(e -> {
        if(jButton1 != null) {
            jButton1.doClick();
        } else {
            System.out.println("Upload button (jButton1) not found!");
        }
    });
}
    
private void setupFooterButton(JButton b) {
    if (b != null) {
        b.setBackground(new Color(60, 63, 65));
        b.setForeground(Color.WHITE);
        b.setPreferredSize(new Dimension(100, 35));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 80)));
        
        
       
    }
}

private void setupButton(JButton b) {
    if (b != null) {
        b.setMaximumSize(new Dimension(200, 40));
        b.setPreferredSize(new Dimension(200, 40));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setBackground(new Color(45, 52, 54));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        b.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 70)));
        b.setVisible(true);
        jPanel2.add(b);
        jPanel2.add(Box.createRigidArea(new Dimension(0, 10))); 
    }
}


private void addSidebarLabel(JLabel lbl) {
    if (lbl != null) {
        lbl.setForeground(new Color(180, 180, 180));
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        jPanel2.add(Box.createRigidArea(new Dimension(0, 15)));
        jPanel2.add(lbl);
        jPanel2.add(Box.createRigidArea(new Dimension(0, 8)));
    }
}


private void styleSidebarButton(JButton b) {
    if (b == null) return;
    
    b.setMaximumSize(new Dimension(210, 45)); 
    b.setPreferredSize(new Dimension(210, 45));
    b.setAlignmentX(Component.LEFT_ALIGNMENT);
    
    
    b.setBackground(new Color(24, 25, 26)); 
    b.setForeground(new Color(228, 230, 235)); 
    b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    b.setFocusPainted(false);
    b.setBorderPainted(false); 
    b.setContentAreaFilled(false); 
    b.setOpaque(true);
    b.setHorizontalAlignment(SwingConstants.LEFT);
    b.setCursor(new Cursor(Cursor.HAND_CURSOR));
    b.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0)); 

   
    b.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseEntered(java.awt.event.MouseEvent e) {
            b.setBackground(new Color(58, 59, 60)); // সফট হোভার কালার
        }
        public void mouseExited(java.awt.event.MouseEvent e) {
            b.setBackground(new Color(24, 25, 26));
        }
    });
}

    
    private void styleLabel(JLabel l) {
        if (l == null) return;
        l.setForeground(new Color(110, 117, 124));
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setBorder(new EmptyBorder(15, 10, 5, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
    }
    private void loadPhotoGallery() {
    if (mainContent == null) return;
    
    mainContent.removeAll();
    int gap = 20; 
    int currentWidth = (scrollPane != null && scrollPane.getWidth() > 0) ? scrollPane.getWidth() : 900;
    
    // ভিউ মোড অনুযায়ী লেআউট সেট করা
    if (isGridView) {
        mainContent.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, gap, gap));
    } else {
        mainContent.setLayout(new javax.swing.BoxLayout(mainContent, javax.swing.BoxLayout.Y_AXIS));
    }
    
    mainContent.setBackground(new Color(18, 18, 18));

    try (java.sql.Connection con = DBConnect.getConnection()) {
        String sql = "SELECT * FROM photo_app WHERE file_path IS NOT NULL AND file_path != '' ORDER BY id DESC";
        java.sql.PreparedStatement pst = con.prepareStatement(sql);
        java.sql.ResultSet rs = pst.executeQuery();

        int photoCount = 0;
        while (rs.next()) {
            final String path = rs.getString("file_path");
            final String tag = rs.getString("tag");
            final int id = rs.getInt("id");

            java.io.File imgFile = new java.io.File(path);
            if (!imgFile.exists() || path.isEmpty()) continue; 

            photoCount++; 

            if (isGridView) {
                // --- GRID VIEW RENDERING ---
                int cardWidth = (currentWidth - (gap * 5)) / 3; 
                JPanel card = new JPanel(new BorderLayout());
                card.setPreferredSize(new Dimension(cardWidth, cardWidth));
                card.setBackground(new Color(33, 34, 35));

                ImageIcon displayIcon = getCachedIcon(path, cardWidth, cardWidth);
                JLabel imgLabel = new JLabel(displayIcon);
                imgLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                imgLabel.setToolTipText("Tag: " + tag);
                imgLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        selectedPhotoId = id; showFullImage(path);
                    }
                });

                card.add(imgLabel, BorderLayout.CENTER);
                mainContent.add(card);
            } else {
                // --- LIST VIEW RENDERING ---
                JPanel row = new JPanel(new BorderLayout(15, 0));
                row.setMaximumSize(new Dimension(currentWidth - 40, 100));
                row.setPreferredSize(new Dimension(currentWidth - 40, 100));
                row.setBackground(new Color(33, 34, 35));
                row.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                // থাম্বনেইল ইমেজ (ছোট সাইজ)
                ImageIcon thumb = getCachedIcon(path, 80, 80);
                JLabel imgLabel = new JLabel(thumb);
                
                // ইনফো প্যানেল
                JPanel info = new JPanel(new java.awt.GridLayout(2, 1));
                info.setOpaque(false);
                JLabel nameLbl = new JLabel("Photo ID: #" + id);
                nameLbl.setForeground(Color.WHITE);
                nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
                JLabel tagLbl = new JLabel("Tag: " + (tag == null || tag.isEmpty() ? "No Tag" : tag));
                tagLbl.setForeground(new Color(150, 150, 150));
                
                info.add(nameLbl);
                info.add(tagLbl);

                row.add(imgLabel, BorderLayout.WEST);
                row.add(info, BorderLayout.CENTER);
                row.setCursor(new Cursor(Cursor.HAND_CURSOR));
                row.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        selectedPhotoId = id; showFullImage(path);
                    }
                });

                mainContent.add(row);
                mainContent.add(Box.createVerticalStrut(10)); // রো এর মাঝে গ্যাপ
            }
        }

        // স্ক্রল প্যানেলের সাইজ ম্যানেজমেন্ট
        if (isGridView) {
            int cardSize = (currentWidth - (gap * 5)) / 3;
            int rows = (int) Math.ceil((double) photoCount / 3);
            mainContent.setPreferredSize(new Dimension(currentWidth - 50, rows * (cardSize + gap) + 100));
        } else {
            mainContent.setPreferredSize(new Dimension(currentWidth - 50, photoCount * 115 + 50));
        }

    } catch (Exception e) { 
        e.printStackTrace();
    }

    mainContent.revalidate();
    mainContent.repaint();
}

// ইমেজ ক্যাশ থেকে লোড করার হেল্পার মেথড (কোড ক্লিন রাখার জন্য)
private ImageIcon getCachedIcon(String path, int w, int h) {
    String cacheKey = path + "_" + w;
    if (imageCache != null && imageCache.containsKey(cacheKey)) {
        return imageCache.get(cacheKey);
    }
    ImageIcon icon = new ImageIcon(path);
    Image img = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
    ImageIcon scaledIcon = new ImageIcon(img);
    if(imageCache != null) imageCache.put(cacheKey, scaledIcon);
    return scaledIcon;
}
private void showFullImage(String path) {
    final int currentPhotoId = selectedPhotoId; 
    JFrame f = new JFrame("Photo Preview");
    f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    f.setSize(1000, 850);
    f.setLocationRelativeTo(null);
    f.setLayout(new BorderLayout());

    // ইমেজ ডিসপ্লে
    ImageIcon icon = new ImageIcon(path);
    Image scaled = icon.getImage().getScaledInstance(900, 550, Image.SCALE_SMOOTH);
    JLabel lbl = new JLabel(new ImageIcon(scaled));
    lbl.setHorizontalAlignment(SwingConstants.CENTER);
    f.add(lbl, BorderLayout.CENTER);

    // অ্যাকশন প্যানেল (ফেভারিট বাটন ছাড়া)
    JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
    actionPanel.setBackground(new Color(245, 245, 245));
    actionPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
        BorderFactory.createEmptyBorder(15, 0, 45, 0)
    ));

    JButton btnTag = new JButton("Add Tag");
    JButton btnShare = new JButton("Share");
    JButton btnDel = new JButton("Delete");

    styleActionBtn(btnTag, new Color(70, 70, 70));
    styleActionBtn(btnShare, new Color(70, 70, 70));
    styleActionBtn(btnDel, new Color(200, 50, 50));

    // ট্যাগ বাটন লজিক
    btnTag.addActionListener(e -> {
        String newTag = JOptionPane.showInputDialog(f, "Enter Tag Name:");
        if (newTag != null && !newTag.trim().isEmpty()) {
            updatePhotoTag(currentPhotoId, newTag); 
            loadPhotoGallery(); 
        }
    });

    // শেয়ার বাটন লজিক (সংক্ষিপ্ত রাখা হয়েছে)
    btnShare.addActionListener(e -> {
        // ... আপনার আগের ইমেইল পাঠানোর লজিক এখানে থাকবে ...
    });

    // ডিলিট বাটন লজিক
    btnDel.addActionListener(e -> {
        if(JOptionPane.showConfirmDialog(f, "Delete this photo?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            deletePhoto(currentPhotoId);
            loadPhotoGallery(); 
            f.dispose();
        }
    });

    actionPanel.add(btnTag);
    actionPanel.add(btnShare);
    actionPanel.add(btnDel);

    f.add(actionPanel, BorderLayout.SOUTH);
    f.setVisible(true);
}
private void copyToClipboard(String text) {
    java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(text);
    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
}
private void styleActionBtn(JButton btn, Color bg) {
    btn.setBackground(bg);
    btn.setForeground(Color.WHITE);
    btn.setFocusPainted(false);
    btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
    btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    btn.setPreferredSize(new Dimension(130, 40));
}
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jButton8 = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jButton11 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jButton5 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setAutoRequestFocus(false);
        setBackground(new java.awt.Color(204, 255, 204));

        jPanel2.setBackground(new java.awt.Color(204, 204, 255));

        jButton1.setText("📤 Upload");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton4.setText("🏷️ Tag");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jLabel3.setText("🚀 MAIN ACTIONS");

        jLabel4.setText("👀 VIEW OPTIONS");

        jButton8.setText("📋 List View");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        jLabel5.setText("🎨 MORE TOOLS");

        jButton11.setText("🗑️ Delete");
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });

        jButton12.setText("📤 Home");
        jButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton12ActionPerformed(evt);
            }
        });

        jButton10.setText("📧 Share");
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton10, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addGap(2, 2, 2)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel4)
                                .addComponent(jButton12, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addComponent(jButton11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jButton8, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addContainerGap(693, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addComponent(jLabel4)
                .addGap(5, 5, 5)
                .addComponent(jButton8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 41, Short.MAX_VALUE)
                .addComponent(jLabel5)
                .addGap(11, 11, 11)
                .addComponent(jButton11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton10))
        );

        jPanel1.setBackground(new java.awt.Color(204, 255, 204));
        jPanel1.setForeground(new java.awt.Color(204, 255, 204));

        jLabel1.setFont(new java.awt.Font("Times New Roman", 1, 24)); // NOI18N
        jLabel1.setText("Photograph Organizer & Tagger");

        jLabel2.setText("🎯 WELCOME TO YOUR PHOTO GALLERY! ");

        jButton5.setText("🔍 Search");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(253, Short.MAX_VALUE)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 359, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(251, 251, 251))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(275, 275, 275)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 301, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2)
                        .addContainerGap(39, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(16, 16, 16))))
        );

        jPanel4.setBackground(new java.awt.Color(204, 255, 204));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 878, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 106, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(35, 35, 35)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
                                          
    JFileChooser chooser = new JFileChooser();
    
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        File selectedFile = chooser.getSelectedFile();
        try {
            
            File dir = new File("photos");
            if (!dir.exists()) {
                dir.mkdir();
            }
            
            
            File dest = new File(dir, System.currentTimeMillis() + "_" + selectedFile.getName());
            
            
            java.nio.file.Files.copy(selectedFile.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            
            Connection con = DBConnect.getConnection();
            String sql = "INSERT INTO photo_app(file_path, tag) VALUES(?, ?)";
            PreparedStatement pst = con.prepareStatement(sql);
            
            pst.setString(1, "photos/" + dest.getName());
            pst.setString(2, "General"); 
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Photo uploaded successfully!");
            con.close();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Upload Error: " + e.getMessage());
        }
    }

       



loadPhotoGallery(); 


this.revalidate();
this.repaint();                                   
    
    
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed

        String query = JOptionPane.showInputDialog(this, "Enter tag to search:");
        if (query != null && !query.trim().isEmpty()) {
            filterByTag(query.trim());
        } else {
            loadPhotoGallery();
        }
    
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton12ActionPerformed
        loadPhotoGallery(); 
    
    }//GEN-LAST:event_jButton12ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        if (selectedPhotoId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a photo first!");
            return;
        }
        String newTag = JOptionPane.showInputDialog(this, "Enter new tag:");
        if (newTag != null && !newTag.trim().isEmpty()) {
            try {
                Connection con = DBConnect.getConnection();
                String sql = "UPDATE photo_app SET tag = ? WHERE id = ?";
                PreparedStatement pst = con.prepareStatement(sql);
                pst.setString(1, newTag);
                pst.setInt(2, selectedPhotoId);
                pst.executeUpdate();
                con.close();
                setupTagFilter(); // ড্রপডাউন রিফ্রেশ
                loadPhotoGallery();
            } catch (Exception e) { e.printStackTrace(); }

        }

    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed

    }//GEN-LAST:event_jButton10ActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        int confirm = javax.swing.JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete this photo?", "Confirm Delete",
            javax.swing.JOptionPane.YES_NO_OPTION);

        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
            try {
                Connection con = DBConnect.getConnection();

                String sql = "DELETE FROM photo_app WHERE id = ?";

                PreparedStatement pst = con.prepareStatement(sql);

                pst.setInt(1, selectedPhotoId);

                int rowsDeleted = pst.executeUpdate();
                con.close();

                if (rowsDeleted > 0) {
                    javax.swing.JOptionPane.showMessageDialog(this, "Photo deleted successfully!");

                    loadPhotoGallery();
                }
            } catch (Exception e) {
                e.printStackTrace();
                javax.swing.JOptionPane.showMessageDialog(this, "Error deleting photo: " + e.getMessage());
            }
        }
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton11ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed

        loadPhotoGallery();
    }//GEN-LAST:event_jButton8ActionPerformed
    

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {
        showGallery(null);
    }

    

    

    

    private void showGallery(String tagFilter) {
        JFrame frame = new JFrame("Photo Gallery");
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        JPanel panel = new JPanel(new GridLayout(0, 4, 10, 10));

        try {
            Connection con = DBConnect.getConnection();
            String sql = (tagFilter == null) ? "SELECT * FROM photo_app" : "SELECT * FROM photo_app WHERE tag LIKE ?";
            PreparedStatement pst = con.prepareStatement(sql);
            if (tagFilter != null) {
                pst.setString(1, "%" + tagFilter + "%");
            }

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                final int id = rs.getInt("id");
                final String path = rs.getString("file_path");

                ImageIcon icon = new ImageIcon(new ImageIcon(path).getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH));
                JLabel label = new JLabel(icon);
                label.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                label.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        selectedPhotoId = id;
                        JOptionPane.showMessageDialog(null, "Photo Selected!");
                    }
                });
                panel.add(label);
            }
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        frame.add(new JScrollPane(panel));
        frame.setVisible(true);
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> {
            new page().setVisible(true);
        });
    }
    /**
     * @param args the command line arguments
     */

private void updatePhotoTag(int id, String tag) {
    if (id == -1) return;
    try {
        Connection con = DBConnect.getConnection();
        String sql = "UPDATE photo_app SET tag = ? WHERE id = ?";
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setString(1, tag);
        pst.setInt(2, id);
        pst.executeUpdate();
        con.close();
        loadPhotoGallery(); // গ্যালারি রিফ্রেশ করবে
        JOptionPane.showMessageDialog(null, "Tag Updated to: " + tag);
    } catch (Exception e) {
        e.printStackTrace();
    }
}

// ৩. ডিলিট করার মেথড
private void deletePhoto(int id) {
    if (id == -1) return;
    try {
        Connection con = DBConnect.getConnection();
        String sql = "DELETE FROM photo_app WHERE id = ?";
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, id);
        pst.executeUpdate();
        con.close();
        loadPhotoGallery(); // ডিলিট হওয়ার পর গ্যালারি থেকে ছবি সরিয়ে দেবে
        JOptionPane.showMessageDialog(null, "Photo Deleted!");
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton8;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    // End of variables declaration//GEN-END:variables


}
