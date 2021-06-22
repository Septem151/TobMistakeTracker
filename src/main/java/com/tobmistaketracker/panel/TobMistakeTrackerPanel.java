package com.tobmistaketracker.panel;

import com.tobmistaketracker.MistakeManager;
import com.tobmistaketracker.TobMistake;
import com.tobmistaketracker.TobMistakeTrackerConfig;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.client.util.SwingUtil;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class TobMistakeTrackerPanel extends PluginPanel {

    private static final String HTML_LABEL_TEMPLATE =
            "<html><body style='color:%s'>%s<span style='color:white'>%s</span></body></html>";

    private final Client client;

    private final MistakeManager currentRaidMistakeManager;
    private final MistakeManager allRaidsMistakeManager;

    // Panel for all actions
    private final JPanel actionsContainer = new JPanel();
    private final JLabel currentViewTitle = new JLabel();
    private final JButton switchMistakesViewBtn = new JButton();

    // Panel for overall mistake data
    private final JPanel overallPanel = new JPanel();
    private final JLabel overallPlayersLabel = new JLabel();
    private final JLabel overallMistakesLabel = new JLabel();
    private final JLabel overallIcon = new JLabel();

    // Panel for all PlayerMistakesBoxes
    private final JPanel mistakesContainer = new JPanel();

    // Keep track of all boxes
    private final List<PlayerMistakesBox> playerMistakesBoxes = new ArrayList<>();

    // Keep track of the current view we're showing
    private boolean isShowingAll = false;

    private final PluginErrorPanel errorPanel = new PluginErrorPanel();

    @Inject
    public TobMistakeTrackerPanel(TobMistakeTrackerConfig config, Client client,
                                  @Named("currentRaidMistakeManager") MistakeManager currentRaidMistakeManager,
                                  @Named("allRaidsMistakeManager") MistakeManager allRaidsMistakeManager) {
        this.client = client;
        this.currentRaidMistakeManager = currentRaidMistakeManager;
        this.allRaidsMistakeManager = allRaidsMistakeManager;

        setBorder(new EmptyBorder(6, 6, 6, 6));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());

        // Add the outer panel for wrapping everything else inside
        final JPanel layoutPanel = new JPanel();
        layoutPanel.setLayout(new BoxLayout(layoutPanel, BoxLayout.Y_AXIS));
        add(layoutPanel, BorderLayout.NORTH);

        // Create panel for the top actions (like switching view button)
        actionsContainer.setLayout(new BorderLayout());
        actionsContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        actionsContainer.setPreferredSize(new Dimension(0, 30));
        actionsContainer.setBorder(new EmptyBorder(5, 5, 5, 10));

        // Create the panel for the buttons
        final JPanel viewButtons = new JPanel(new GridLayout(1, 3, 10, 0));
        viewButtons.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Create the panel for the current view title
        final JPanel leftTitleContainer = new JPanel(new BorderLayout(5, 0));
        leftTitleContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Create the current view title
        currentViewTitle.setForeground(Color.WHITE);
        currentViewTitle.setText(getCurrentViewTitleText());
        leftTitleContainer.add(currentViewTitle, BorderLayout.CENTER);

        // Create the switch view button
        SwingUtil.removeButtonDecorations(switchMistakesViewBtn);
        switchMistakesViewBtn.setText(getSwitchMistakesViewButtonText());
        switchMistakesViewBtn.setBackground(Color.WHITE);
        switchMistakesViewBtn.setBorder(new EmptyBorder(10 ,10 ,10 ,10));
        switchMistakesViewBtn.setBorderPainted(true);
        switchMistakesViewBtn.setPreferredSize(new Dimension(100, 10));

        // Add the listeners to the button
        switchMistakesViewBtn.addActionListener(e -> switchMistakesView());
        switchMistakesViewBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                switchMistakesViewBtn.setForeground(Color.LIGHT_GRAY);
                switchMistakesViewBtn.setBackground(Color.LIGHT_GRAY);
            }

            public void mouseExited(MouseEvent e) {
                switchMistakesViewBtn.setForeground(Color.WHITE);
                switchMistakesViewBtn.setBackground(Color.WHITE);
            }
        });
        viewButtons.add(switchMistakesViewBtn);

        // Add all our panels to the top container for actions
        actionsContainer.add(leftTitleContainer, BorderLayout.CENTER);
        actionsContainer.add(viewButtons, BorderLayout.EAST);
        actionsContainer.setVisible(true);

        // Create panel that will contain overall data (at the top)
        overallPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(5, 0, 0, 0, ColorScheme.DARK_GRAY_COLOR),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        overallPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        overallPanel.setLayout(new BorderLayout());

        // Add icon and contents to overallPanel
        final JPanel overallInfo = new JPanel();
        overallInfo.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        overallInfo.setLayout(new GridLayout(2, 1));
        overallInfo.setBorder(new EmptyBorder(2, 10, 2, 0));
        overallPlayersLabel.setFont(FontManager.getRunescapeSmallFont());
        overallMistakesLabel.setFont(FontManager.getRunescapeSmallFont());
        overallInfo.add(overallPlayersLabel);
        overallInfo.add(overallMistakesLabel);
        overallPanel.add(overallIcon, BorderLayout.WEST);
        overallPanel.add(overallInfo, BorderLayout.CENTER);

        // Create reset all menu
        final JMenuItem reset = new JMenuItem("Reset All");
        reset.addActionListener(e ->
        {
            final int result = JOptionPane.showOptionDialog(overallPanel,
                    "This will permanently delete ALL mistakes across ALL raids from the client.",
                    "Are you sure?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, new String[]{"Yes", "No"}, "No");

            if (result != JOptionPane.YES_OPTION) {
                return;
            }

            resetAll();
        });

        // Create popup menu
        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(new EmptyBorder(5, 5, 5, 5));
        popupMenu.add(reset);
        overallPanel.setComponentPopupMenu(popupMenu);

        // Create the mistakes panel which shows all mistakes for all players
        mistakesContainer.setLayout(new BoxLayout(mistakesContainer, BoxLayout.Y_AXIS));

        // Add all our panels in the order we want them to appear
        layoutPanel.add(actionsContainer);
        layoutPanel.add(overallPanel);
        layoutPanel.add(mistakesContainer);

        // Create testing button
        if (config.isDebug()) {
            JButton testButton = new JButton("Test Mistakes");
            testButton.addActionListener(e ->
            {
                for (TobMistake mistake : TobMistake.values()) {
                    addMistakeForPlayer("Test Player", mistake);
                }
            });
            layoutPanel.add(testButton);
        }

        // Add the error for when there are no mistakes yet
        errorPanel.setContent("Tob Mistake Tracker", "You have not tracked any mistakes yet.");
        updateVisiblePanels(true);
    }

    public void loadHeaderIcon(BufferedImage img) {
        overallIcon.setIcon(new ImageIcon(img));
    }

    /**
     * Resets the current raid mistakes and panel
     */
    public void resetCurrentRaid() {
        currentRaidMistakeManager.clearAllMistakes();
        if (!isShowingAll) {
            // We're looking at the current raid view
            resetUi();
        }
    }

    /**
     * Adds a mistake for the specified player, both in the manager and the panel.
     *
     * @param playerName - The player name that a mistake was added for
     */
    public void addMistakeForPlayer(String playerName, TobMistake mistake) {
        // Always add to both
        currentRaidMistakeManager.addMistakeForPlayer(playerName, mistake);
        allRaidsMistakeManager.addMistakeForPlayer(playerName, mistake);

        PlayerMistakesBox box = buildBox(playerName);
        box.rebuildAllMistakes();
        updateOverallPanel();
    }

    /**
     * Removes a mistake for the specified player, both in the manager and the panel. This is only callable from
     * the box's mistake's Reset action.
     *
     * @param playerName - The player name that a mistake was added for
     */
    // TODO: This is currently unused as I'm not sure I even want this feature.
    private void removeMistakeForPlayer(String playerName, TobMistake mistake) {
        getMistakeManager().removeMistakeForPlayer(playerName, mistake);

        for (PlayerMistakesBox box : playerMistakesBoxes) {
            if (box.getPlayerName().equals(playerName)) {
                box.rebuildAllMistakes();
                updateOverallPanel();
                return;
            }
        }
    }

    /**
     * Rebuilds all the boxes from scratch based on which view we're currently looking at
     */
    private void rebuildAll() {
        SwingUtil.fastRemoveAll(mistakesContainer);
        playerMistakesBoxes.clear();

        for (String playerName : getMistakeManager().getPlayersWithMistakes()) {
            buildBox(playerName);
        }

        playerMistakesBoxes.forEach(PlayerMistakesBox::rebuildAllMistakes);
        updateOverallPanel();
        mistakesContainer.revalidate();
        mistakesContainer.repaint();
    }

    private PlayerMistakesBox buildBox(String playerName) {
        for (PlayerMistakesBox box : playerMistakesBoxes) {
            if (box.getPlayerName().equals(playerName)) {
                if (client.getLocalPlayer() != null && playerName.equals(client.getLocalPlayer().getName())) {
                    // This existing box is for me, make sure it goes first if it somehow isn't already
                    mistakesContainer.setComponentZOrder(box, 0);
                } else if (doesLocalPlayerHaveMistakesBox()) {
                    // I already have some mistakes, so this should go right after
                    mistakesContainer.setComponentZOrder(box, 1);
                } else {
                    // It's not for me, and I have no mistakes, it can go in the front
                    mistakesContainer.setComponentZOrder(box, 0);
                }
                return box;
            }
        }

        // Create a new box if one could not be found
        PlayerMistakesBox box = new PlayerMistakesBox(getMistakeManager(), playerName);

        // Use the existing popup menu or create a new one
        JPopupMenu popupMenu = box.getComponentPopupMenu();
        if (popupMenu == null) {
            popupMenu = new JPopupMenu();
            popupMenu.setBorder(new EmptyBorder(5, 5, 5, 5));
            box.setComponentPopupMenu(popupMenu);
        }

        // Create reset menu
        final JMenuItem reset = new JMenuItem("Reset ALL Mistakes for " + playerName);
        reset.addActionListener(e -> {
            // Always remove from both
            currentRaidMistakeManager.removeMistakesForPlayer(playerName);
            allRaidsMistakeManager.removeMistakesForPlayer(playerName);
            playerMistakesBoxes.remove(box);

            updateOverallPanel();
            mistakesContainer.remove(box);
            mistakesContainer.repaint();

            if (playerMistakesBoxes.isEmpty()) {
                updateVisiblePanels(true);
            }
        });

        popupMenu.add(reset);

        // Show main view
        updateVisiblePanels(false);

        // Add box to panel
        if (client.getLocalPlayer() != null && playerName.equals(client.getLocalPlayer().getName())) {
            // This box is for me, put at the front
            mistakesContainer.add(box, 0);
        } else if (doesLocalPlayerHaveMistakesBox()) {
            // I already have some mistakes, so this should go right after
            mistakesContainer.add(box, 1);
        } else {
            // It's not for me, and I have no mistakes, it can go in the front
            mistakesContainer.add(box, 0);
        }
        playerMistakesBoxes.add(box);

        return box;
    }

    private void resetAll() {
        currentRaidMistakeManager.clearAllMistakes();
        allRaidsMistakeManager.clearAllMistakes();
        resetUi();
    }

    private void resetUi() {
        playerMistakesBoxes.clear();

        updateOverallPanel();
        mistakesContainer.removeAll();
        mistakesContainer.repaint();

        updateVisiblePanels(true);
    }

    private void updateOverallPanel() {
        overallPlayersLabel.setText(htmlLabel("Total players: ",
                getMistakeManager().getPlayersWithMistakes().size()));
        overallMistakesLabel.setText(htmlLabel("Total mistakes: ",
                getMistakeManager().getTotalMistakesForAllPlayers()));
    }

    private static String htmlLabel(String key, long value) {
        final String valueStr = QuantityFormatter.quantityToStackSize(value);
        return String.format(HTML_LABEL_TEMPLATE, ColorUtil.toHexColor(ColorScheme.LIGHT_GRAY_COLOR), key, valueStr);
    }

    private boolean doesLocalPlayerHaveMistakesBox() {
        if (client.getLocalPlayer() != null) {
            String name = client.getLocalPlayer().getName();
            return playerMistakesBoxes.stream().anyMatch(b -> b.getPlayerName().equals(name));
        }

        return false;
    }

    private void updateVisiblePanels(boolean isEmpty) {
        if (isEmpty) {
            add(errorPanel);
            overallPanel.setVisible(false);
            mistakesContainer.setVisible(false);
        } else {
            remove(errorPanel);
            overallPanel.setVisible(true);
            mistakesContainer.setVisible(true);
        }
    }

    private void switchMistakesView() {
        // TODO: Minor UI bug: Switching panels re-orders mistakes on the first switch, and is consistent afterwards
        // until another mistake is added. This is because we re-order the boxes we add, but don't persist that ordering
        // in the underlying manager.
        isShowingAll = !isShowingAll;
        currentViewTitle.setText(getCurrentViewTitleText());
        switchMistakesViewBtn.setText(getSwitchMistakesViewButtonText());
        rebuildAll();
    }

    private String getCurrentViewTitleText() {
        return isShowingAll ? "All Raids" : "Current Raid";
    }

    private String getSwitchMistakesViewButtonText() {
        return isShowingAll ? "Show Current" : "Show All";
    }

    private MistakeManager getMistakeManager() {
        return isShowingAll ? allRaidsMistakeManager : currentRaidMistakeManager;
    }
}