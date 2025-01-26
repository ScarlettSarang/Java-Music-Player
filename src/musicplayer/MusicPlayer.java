package musicplayer; // Declares the package name for the class. 

import javax.swing.*; // Imports Swing components for GUI.
import javax.swing.filechooser.FileSystemView; // Allows file chooser to view file system.
import java.awt.*; // Imports AWT components for layouts and GUI.
import java.io.*; // Imports I/O classes for file handling.
import java.util.ArrayList; // Imports ArrayList for managing the playlist.
import javax.sound.sampled.*; // Imports classes for audio handling.

public class MusicPlayer extends JFrame // Defines the MusicPlayer class extending JFrame for GUI.
{
    // GUI components and class variables.
    private final JLabel titleLabel; // Label to display the music title.
    private final JLabel timeLabel; // Label to display the music time.
    private JSlider slider; // Slider to show and control music progress.
    private final JButton playButton, pauseButton, nextButton, prevButton, loopButton; // Buttons for controls.
    private boolean isPlaying = false; // Tracks if music is currently playing. (==== Member 1 - Controls the playback state ====)
    private boolean isPaused = false; // Tracks if music is currently paused. (==== Member 1 - Indicates if music is paused ====)
    private boolean isLooping = false; // Tracks if music is in looping mode. (==== Member 1 - Controls the looping feature ====)
    private Clip clip; // Clip object to handle audio playback. (==== Member 1 - Audio playback control ====)
    private javax.swing.Timer sliderTimer; // Timer for updating the slider's position. (==== Member 1 - Updates the progress slider every second ====)
    private final ArrayList<File> playlist = new ArrayList<>(); // Stores the playlist files. (==== Member 2 - Stores the list of songs ====)
    private int currentTrackIndex = 0; // Index of the currently playing track. (==== Member 2 - Keeps track of the current song in the playlist ====)
  
    public MusicPlayer() // Constructor to initialize the GUI and components.
    {
        // ==== Member 3 - User Interface setup (buttons, sliders, labels) ====
        setTitle("Music Player"); // Sets the window title.
        setSize(600, 400); // Sets the window size.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Sets default close operation.
        setLayout(new BorderLayout()); // Sets the layout for the JFrame.

        // Top panel for header and labels.
        JPanel topPanel = new JPanel(new GridLayout(4, 1, 10, 10)); // Panel with GridLayout for components.

        JLabel headerLabel = new JLabel("Music Player", SwingConstants.CENTER); // Header label centered.
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24)); // Sets font for the header.
        topPanel.add(headerLabel); // Adds the header to the top panel.

        titleLabel = new JLabel("Title: No Music Playing", SwingConstants.CENTER); // Initializes the title label. (==== Member 3 - Title label ====)
        topPanel.add(titleLabel); // Adds title label to the top panel.

        timeLabel = new JLabel("Time: 00:00 / 00:00", SwingConstants.CENTER); // Initializes the time label. (==== Member 3 - Time label ====)
        topPanel.add(timeLabel); // Adds time label to the top panel.

        add(topPanel, BorderLayout.NORTH); // Adds the top panel to the JFrame.

        // ==== Member 3 - Slider ====
        slider = new JSlider(0, 100, 0); // Initializes the slider for progress.
        slider.setEnabled(false); // Disables the slider initially.
        slider.addChangeListener(e -> // Adds a change listener for the slider.
        {
            if (slider.getValueIsAdjusting() && clip != null) // Checks if the slider is being adjusted and clip is not null.
            {
                int newPosition = slider.getValue() * 1000; // Converts the slider value to microseconds.
                clip.setMicrosecondPosition(newPosition); // Sets the clip position. (==== Member 1 - Adjust playback position based on slider ====)
                updateSlider(); // Updates the slider position.
            }
        });
        add(slider, BorderLayout.CENTER); // Adds the slider to the center of the JFrame.

        // Control panel for buttons.
        JPanel controlPanel = new JPanel(new FlowLayout()); // Panel with FlowLayout for buttons.

        // Initializes control buttons. (==== Member 1 ====)
        playButton = new JButton("Play");
        pauseButton = new JButton("Pause");
        nextButton = new JButton("Next");
        prevButton = new JButton("Previous");
        loopButton = new JButton("Loop");

        // Adds buttons to the control panel.
        controlPanel.add(prevButton);
        controlPanel.add(playButton);
        controlPanel.add(pauseButton);
        controlPanel.add(nextButton);
        controlPanel.add(loopButton);

        add(controlPanel, BorderLayout.SOUTH); // Adds the control panel to the JFrame.

        // ==== Member 4 - Option to load playlist folder ====
        JMenuBar menuBar = new JMenuBar(); // Creates a menu bar.
        JMenu fileMenu = new JMenu("Load Playlist"); // Menu for loading playlist.
        JMenuItem openFolder = new JMenuItem("Open Folder"); // Menu item for opening folder.
        fileMenu.add(openFolder); // Adds menu item to the menu.
        menuBar.add(fileMenu); // Adds menu to the menu bar.
        setJMenuBar(menuBar); // Sets the menu bar for the JFrame.

        // ==== Member 1, 2 - Add action listeners to buttons and menu items for functionality ====
        openFolder.addActionListener(e -> openFolder()); // Opens a folder dialog.
        playButton.addActionListener(e -> playMusic()); // Member 1 - Plays music.
        pauseButton.addActionListener(e -> pauseMusic()); // Member 1 - Pauses music.
        nextButton.addActionListener(e -> playNextTrack()); // Member 2 - Plays the next track.
        prevButton.addActionListener(e -> playPreviousTrack()); // Member 2 - Plays the previous track.
        loopButton.addActionListener(e -> toggleLoop()); // Member 1 - Toggles looping mode.
    }

    // ==== Member 4 - Open folder to load playlist of music files ====
    private void openFolder() // Opens a folder dialog to select a music folder.
    {
        JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView()); // File chooser instance.
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Allows only folder selection.

        int returnValue = fileChooser.showOpenDialog(null); // Opens the folder dialog.
        if (returnValue == JFileChooser.APPROVE_OPTION) { // Checks if a folder was selected.
            File selectedFolder = fileChooser.getSelectedFile(); // Gets the selected folder.
            loadPlaylist(selectedFolder); // Loads the playlist from the folder.
        }
    }

    // ==== Member 4 - Load the playlist from the selected folder ====
    private void loadPlaylist(File folder) // Loads WAV files from the selected folder into the playlist.
    {
        playlist.clear(); // Clears the existing playlist.
        for (File file : folder.listFiles()) // Iterates through the folder's files.
        {
            if (file.getName().endsWith(".wav")) // Checks if the file is a WAV file.
            {
                playlist.add(file); // Adds the WAV file to the playlist.
            }
        }

        if (!playlist.isEmpty()) // Checks if the playlist is not empty.
        {
            currentTrackIndex = 0; // Resets the track index.
            playMusic(); // Starts playing the music. (==== Member 1 - Automatically play first track if playlist is not empty ====)
        } 
        else // If no WAV files are found.
        {
            JOptionPane.showMessageDialog(this, "No WAV files found in the selected folder.", "Error", JOptionPane.ERROR_MESSAGE); // Shows an error message.
        }
    }

    // ==== Member 1 - Play the current track ====
    private void playMusic() // Plays the current track in the playlist.
    {
        if (playlist.isEmpty()) // Checks if the playlist is empty.
        {
            JOptionPane.showMessageDialog(this, "No music to play.", "Error", JOptionPane.ERROR_MESSAGE); // Shows an error message.
            return; // Exits the method.
        }

        try // Handles potential exceptions.
        {
            if (isPaused && clip != null) // Checks if playback is paused.
            {
                clip.start(); // Resumes playback.
                isPaused = false; // Updates paused status.
                isPlaying = true; // Updates playing status.

                // Restarts the slider timer.
                if (sliderTimer != null) 
                {
                    sliderTimer.start();
                }
                return;
            }

            if (clip != null && clip.isRunning()) // Stops the currently playing clip if any.
            {
                clip.stop();
                clip.close();
            }

            File track = playlist.get(currentTrackIndex); // Gets the current track.
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(track); // Loads the track audio stream.
            clip = AudioSystem.getClip(); // Initializes the clip.
            clip.open(audioStream); // Opens the audio stream in the clip.

            clip.addLineListener(event -> // Adds a listener for the clip.
            {
                if (event.getType() == LineEvent.Type.STOP) // Handles the stop event.
                {
                    if (isLooping) // Checks if looping is enabled.
                    {
                        clip.setFramePosition(0); // Resets clip to the start.
                        clip.start(); // Restarts playback.
                    } 
                    else 
                    {
                        sliderTimer.stop(); // Stops the slider timer.
                    }
                }
            });

            titleLabel.setText("Title: " + track.getName()); // Sets the title label with track name.
            timeLabel.setText("Time: 00:00 / " + formatTime(clip.getMicrosecondLength() / 1_000_000)); // Updates the time label.

            slider.setMaximum((int) (clip.getMicrosecondLength() / 1_000)); // Sets slider max based on clip length.
            slider.setValue(0); // Resets slider value.
            slider.setEnabled(true); // Enables the slider.

            sliderTimer = new javax.swing.Timer(1000, e -> updateSlider()); // Initializes a timer for slider updates.
            sliderTimer.start(); // Starts the slider timer.

            clip.start(); // Starts playback.
            isPlaying = true; // Updates playing status.
        } 
        catch (Exception ex) // Catches and handles exceptions.
        {
            JOptionPane.showMessageDialog(this, "Error playing music: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); // Shows an error message.
        }
    }

    // ==== Member 1 - Pause the music ====
    private void pauseMusic() // Pauses the current track.
    {
        if (clip != null && clip.isRunning()) // Checks if the clip is playing.
        {
            clip.stop(); // Stops the clip.
            isPaused = true; // Updates paused status.

            // Stops the slider timer when paused.
            if (sliderTimer != null) 
            {
                sliderTimer.stop();
            }
        }
    }

    // ==== Member 2 - Play next track in playlist ====
    private void playNextTrack() // Plays the next track in the playlist.
    {
        if (!playlist.isEmpty() && currentTrackIndex < playlist.size() - 1) // Checks if there is a next track.
        {
            currentTrackIndex++; // Increments the track index.
            playMusic(); // Plays the next track.
        }
    }

    // ==== Member 2 - Play previous track in playlist ====
    private void playPreviousTrack() // Plays the previous track in the playlist.
    {
        if (!playlist.isEmpty() && currentTrackIndex > 0) // Checks if there is a previous track.
        {
            currentTrackIndex--; // Decrements the track index.
            playMusic(); // Plays the previous track.
        }
    }

    // ==== Member 1 - Toggle loop mode ====
    private void toggleLoop() // Toggles looping mode for playback.
    {
        isLooping = !isLooping; // Toggles looping status.
        loopButton.setText(isLooping ? "Loop: ON" : "Loop: OFF"); // Updates the loop button text.
    }

    // ==== Member 3 - Update the slider and time label ====
    private void updateSlider() // Updates the slider position and time label during playback.
    {
        if (clip != null && clip.isRunning()) // Checks if the clip is playing.
        {
            int currentPosition = (int) (clip.getMicrosecondPosition() / 1_000); // Gets the current position.
            slider.setValue(currentPosition); // Updates the slider value.
            timeLabel.setText("Time: " + formatTime(currentPosition / 1000) + " / " + formatTime(slider.getMaximum() / 1000)); // Updates the time label.
        }
    }

    // ==== Member 3 - Format time for display ====
    private String formatTime(long seconds) // Formats seconds into mm:ss format.
    {
        long mins = seconds / 60; // Calculates minutes.
        long secs = seconds % 60; // Calculates remaining seconds.
        return String.format("%02d:%02d", mins, secs); // Returns formatted time.
    }

    public static void main(String[] args) // Main method to launch the application.
    {
        SwingUtilities.invokeLater(() -> // Ensures GUI updates on the Event Dispatch Thread.
        {
            MusicPlayer player = new MusicPlayer(); // Creates an instance of MusicPlayer.
            player.setVisible(true); // Makes the JFrame visible.
        });
    }
}
