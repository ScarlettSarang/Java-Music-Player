package musicplayer;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import javax.sound.sampled.*;

public class MusicPlayer extends JFrame 
{

    private JLabel titleLabel;
    private JLabel timeLabel;
    private JSlider slider;
    private JButton playButton, pauseButton, nextButton, prevButton, loopButton;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private boolean isLooping = false;
    private Clip clip;
    private javax.swing.Timer sliderTimer; // Fully qualified name to avoid ambiguity
    private ArrayList<File> playlist = new ArrayList<>();
    private int currentTrackIndex = 0;

    public MusicPlayer() 
    {
        setTitle("Music Player");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridLayout(4, 1, 10, 10));

        JLabel headerLabel = new JLabel("Music Player", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        topPanel.add(headerLabel);

        titleLabel = new JLabel("Title: No Music Playing", SwingConstants.CENTER);
        topPanel.add(titleLabel);

        timeLabel = new JLabel("Time: 00:00 / 00:00", SwingConstants.CENTER);
        topPanel.add(timeLabel);

        add(topPanel, BorderLayout.NORTH);

        slider = new JSlider(0, 100, 0);
        slider.setEnabled(false);
        slider.addChangeListener(e -> 
        {
            if (slider.getValueIsAdjusting() && clip != null) 
            {
                int newPosition = slider.getValue() * 1000; // Convert to microseconds
                clip.setMicrosecondPosition(newPosition);
                updateSlider();
            }
        });
        add(slider, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout());

        playButton = new JButton("Play");
        pauseButton = new JButton("Pause");
        nextButton = new JButton("Next");
        prevButton = new JButton("Previous");
        loopButton = new JButton("Loop");

        controlPanel.add(prevButton);
        controlPanel.add(playButton);
        controlPanel.add(pauseButton);
        controlPanel.add(nextButton);
        controlPanel.add(loopButton);

        add(controlPanel, BorderLayout.SOUTH);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Load Playlist");
        JMenuItem openFolder = new JMenuItem("Open Folder");
        fileMenu.add(openFolder);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        openFolder.addActionListener(e -> openFolder());
        playButton.addActionListener(e -> playMusic());
        pauseButton.addActionListener(e -> pauseMusic());
        nextButton.addActionListener(e -> playNextTrack());
        prevButton.addActionListener(e -> playPreviousTrack());
        loopButton.addActionListener(e -> toggleLoop());
    }

    private void openFolder() 
    {
        JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView());
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int returnValue = fileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileChooser.getSelectedFile();
            loadPlaylist(selectedFolder);
        }
    }

    private void loadPlaylist(File folder) 
    {
        playlist.clear();
        for (File file : folder.listFiles()) 
        {
            if (file.getName().endsWith(".wav")) 
            {
                playlist.add(file);
            }
        }

        if (!playlist.isEmpty()) 
        {
            currentTrackIndex = 0;
            playMusic();
        } 
        else 
        {
            JOptionPane.showMessageDialog(this, "No WAV files found in the selected folder.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

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
                clip.start();
                isPaused = false;
                isPlaying = true;
                return;
            }

            if (clip != null && clip.isRunning()) 
            {
                clip.stop();
                clip.close();
            }

            File track = playlist.get(currentTrackIndex);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(track);
            clip = AudioSystem.getClip();
            clip.open(audioStream);

            clip.addLineListener(event -> 
            {
                if (event.getType() == LineEvent.Type.STOP) 
                {
                    if (isLooping) 
                    {
                        clip.setFramePosition(0);
                        clip.start();
                    } 
                    else 
                    {
                        sliderTimer.stop();
                    }
                }
            });

            titleLabel.setText("Title: " + track.getName());
            timeLabel.setText("Time: 00:00 / " + formatTime(clip.getMicrosecondLength() / 1_000_000));

            slider.setMaximum((int) (clip.getMicrosecondLength() / 1_000));
            slider.setValue(0);
            slider.setEnabled(true);

            sliderTimer = new javax.swing.Timer(1000, e -> updateSlider());
            sliderTimer.start();

            clip.start();
            isPlaying = true;
        } 
        catch (Exception ex) 
        {
            JOptionPane.showMessageDialog(this, "Error playing music: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void pauseMusic() 
    {
        if (clip != null && clip.isRunning()) 
        {
            clip.stop();
            isPaused = true;
        }
    }

    private void playNextTrack() 
    {
        if (!playlist.isEmpty() && currentTrackIndex < playlist.size() - 1) 
        {
            currentTrackIndex++;
            playMusic();
        }
    }

    private void playPreviousTrack() 
    {
        if (!playlist.isEmpty() && currentTrackIndex > 0) 
        {
            currentTrackIndex--;
            playMusic();
        }
    }

    private void toggleLoop() {
        isLooping = !isLooping;
        loopButton.setText(isLooping ? "Loop: ON" : "Loop: OFF");
    }

    private void updateSlider() 
    {
        if (clip != null && clip.isRunning()) 
        {
            int currentPosition = (int) (clip.getMicrosecondPosition() / 1_000);
            slider.setValue(currentPosition);
            timeLabel.setText("Time: " + formatTime(currentPosition / 1000) + " / " + formatTime(slider.getMaximum() / 1000));
        }
    }

    private String formatTime(long seconds) 
    {
        long mins = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(() -> 
        {
            MusicPlayer player = new MusicPlayer();
            player.setVisible(true);
        });
    }
}
