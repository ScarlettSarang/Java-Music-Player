package musicplayer;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import javax.sound.sampled.*;

public class MusicPlayer extends JFrame 
{
    private final JLabel titleLabel;
    private final JLabel timeLabel;
    private JSlider slider;
    private final JButton playButton, pauseButton, nextButton, prevButton, loopButton;
    private boolean isPlaying = false; // Member 1 - Controls the playback state
    private boolean isPaused = false; // Member 1 - Indicates if music is paused
    private boolean isLooping = false; // Member 1 - Controls the looping feature
    private Clip clip; // Member 1 - Audio playback control
    private javax.swing.Timer sliderTimer; // Member 1 - Updates the progress slider every second
    private final ArrayList<File> playlist = new ArrayList<>(); // Member 2 - Stores the list of songs
    private int currentTrackIndex = 0; // Member 2 - Keeps track of the current song in the playlist

    public MusicPlayer() 
    {
        // Member 3 - User Interface setup (buttons, sliders, labels)
        setTitle("Music Player");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridLayout(4, 1, 10, 10));

        JLabel headerLabel = new JLabel("Music Player", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        topPanel.add(headerLabel);

        titleLabel = new JLabel("Title: No Music Playing", SwingConstants.CENTER); // Member 3 - Title label
        topPanel.add(titleLabel);

        timeLabel = new JLabel("Time: 00:00 / 00:00", SwingConstants.CENTER); // Member 3 - Time label
        topPanel.add(timeLabel);

        add(topPanel, BorderLayout.NORTH);

        slider = new JSlider(0, 100, 0); // Member 3 - Slider to show music progress
        slider.setEnabled(false); // Initially disabled until music starts
        slider.addChangeListener(e -> {
            if (slider.getValueIsAdjusting() && clip != null) {
                int newPosition = slider.getValue() * 1000; // Convert to microseconds
                clip.setMicrosecondPosition(newPosition); // Member 1 - Adjust playback position based on slider
                updateSlider();
            }
        });
        add(slider, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout());

        playButton = new JButton("Play"); // Member 1 - Play button
        pauseButton = new JButton("Pause"); // Member 1 - Pause button
        nextButton = new JButton("Next"); // Member 2 - Next song button
        prevButton = new JButton("Previous"); // Member 2 - Previous song button
        loopButton = new JButton("Loop"); // Member 1 - Loop button

        controlPanel.add(prevButton);
        controlPanel.add(playButton);
        controlPanel.add(pauseButton);
        controlPanel.add(nextButton);
        controlPanel.add(loopButton);

        add(controlPanel, BorderLayout.SOUTH);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Load Playlist");
        JMenuItem openFolder = new JMenuItem("Open Folder"); // Member 4 - Option to load playlist folder
        fileMenu.add(openFolder);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Member 1, 2 - Add action listeners to buttons for functionality
        openFolder.addActionListener(e -> openFolder());
        playButton.addActionListener(e -> playMusic()); // Member 1 - Play music
        pauseButton.addActionListener(e -> pauseMusic()); // Member 1 - Pause music
        nextButton.addActionListener(e -> playNextTrack()); // Member 2 - Next track
        prevButton.addActionListener(e -> playPreviousTrack()); // Member 2 - Previous track
        loopButton.addActionListener(e -> toggleLoop()); // Member 1 - Toggle loop mode
    }

    // Member 4 - Open folder to load playlist of music files
    private void openFolder() 
    {
        JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView());
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int returnValue = fileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileChooser.getSelectedFile();
            loadPlaylist(selectedFolder); // Member 4 - Load the music files in the selected folder
        }
    }

    // Member 2 - Load the playlist from the selected folder
    private void loadPlaylist(File folder) 
    {
        playlist.clear(); // Clear existing playlist
        for (File file : folder.listFiles()) 
        {
            if (file.getName().endsWith(".wav")) 
            {
                playlist.add(file); // Add WAV files to the playlist
            }
        }

        if (!playlist.isEmpty()) 
        {
            currentTrackIndex = 0; // Start with the first track
            playMusic(); // Member 1 - Automatically play first track if playlist is not empty
        } 
        else 
        {
            JOptionPane.showMessageDialog(this, "No WAV files found in the selected folder.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Member 1 - Play the current track
    private void playMusic() 
    {
        if (playlist.isEmpty()) 
        {
            JOptionPane.showMessageDialog(this, "No music to play.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try 
            {
            if (isPaused && clip != null) 
            {
                clip.start(); // Resume playback if paused
                isPaused = false;
                isPlaying = true;
                return;
            }

            if (clip != null && clip.isRunning()) 
            {
                clip.stop();
                clip.close(); // Stop and close current clip if it's playing
            }

            File track = playlist.get(currentTrackIndex); // Get the current track
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(track);
            clip = AudioSystem.getClip();
            clip.open(audioStream); // Open audio stream for the track

            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) 
                {
                    if (isLooping) 
                    {
                        clip.setFramePosition(0); // Start from the beginning if looping
                        clip.start();
                    } 
                    else 
                    {
                        sliderTimer.stop(); // Stop slider timer when track ends
                    }
                }
            });

            titleLabel.setText("Title: " + track.getName()); // Display track title
            timeLabel.setText("Time: 00:00 / " + formatTime(clip.getMicrosecondLength() / 1_000_000)); // Display track duration

            slider.setMaximum((int) (clip.getMicrosecondLength() / 1_000)); // Set slider max to track length
            slider.setValue(0);
            slider.setEnabled(true);

            sliderTimer = new javax.swing.Timer(1000, e -> updateSlider()); // Update slider every second
            sliderTimer.start();

            clip.start(); // Start playback
            isPlaying = true;
        } 
        catch (Exception ex) 
            {
            JOptionPane.showMessageDialog(this, "Error playing music: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Member 1 - Pause the music
    private void pauseMusic() 
    {
        if (clip != null && clip.isRunning()) 
        {
            clip.stop();
            isPaused = true;
        }
    }

    // Member 2 - Play next track in playlist
    private void playNextTrack() 
    {
        if (!playlist.isEmpty() && currentTrackIndex < playlist.size() - 1) 
        {
            currentTrackIndex++;
            playMusic(); // Play next track
        }
    }

    // Member 2 - Play previous track in playlist
    private void playPreviousTrack() 
    {
        if (!playlist.isEmpty() && currentTrackIndex > 0) 
        {
            currentTrackIndex--;
            playMusic(); // Play previous track
        }
    }

    // Member 1 - Toggle loop mode
    private void toggleLoop() 
    {
        isLooping = !isLooping;
        loopButton.setText(isLooping ? "Loop: ON" : "Loop: OFF");
    }

    // Member 1 - Update the slider and time label
    private void updateSlider() 
    {
        if (clip != null && clip.isRunning()) 
        {
            int currentPosition = (int) (clip.getMicrosecondPosition() / 1_000);
            slider.setValue(currentPosition);
            timeLabel.setText("Time: " + formatTime(currentPosition / 1000) + " / " + formatTime(slider.getMaximum() / 1000));
        }
    }

    // Member 1 - Format time for display
    private String formatTime(long seconds) 
    {
        long mins = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(() -> {
            MusicPlayer player = new MusicPlayer();
            player.setVisible(true);
        });
    }
}
