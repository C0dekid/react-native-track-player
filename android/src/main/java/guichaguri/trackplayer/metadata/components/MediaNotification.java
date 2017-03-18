package guichaguri.trackplayer.metadata.components;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat.Action;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.support.v7.app.NotificationCompat.MediaStyle;
import com.facebook.react.bridge.ReadableMap;
import guichaguri.trackplayer.logic.PlayerService;
import guichaguri.trackplayer.logic.Utils;

/**
 * @author Guilherme Chaguri
 */
public class MediaNotification {

    private final Context context;

    private NotificationCompat.Builder nb;
    private MediaStyle style;

    private int playIcon, pauseIcon, stopIcon, previousIcon, nextIcon;
    private Action play, pause, stop, previous, next;

    private boolean showing = false;

    public MediaNotification(Context context, MediaSessionCompat session) {
        this.context = context;

        this.nb = new NotificationCompat.Builder(context);
        this.style = new MediaStyle().setMediaSession(session.getSessionToken());

        nb.setStyle(style);
        nb.setCategory(NotificationCompat.CATEGORY_TRANSPORT);
        nb.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        nb.setDeleteIntent(createActionIntent(PlaybackStateCompat.ACTION_STOP));
        nb.setContentIntent(session.getController().getSessionActivity());
    }

    @SuppressWarnings("ResourceAsColor")
    public void updateOptions(ReadableMap data) {
        // Load the icons
        playIcon = loadIcon(data, "playIcon", "play");
        pauseIcon = loadIcon(data, "pauseIcon", "pause");
        stopIcon = loadIcon(data, "stopIcon", "stop");
        previousIcon = loadIcon(data, "previousIcon", "previous");
        nextIcon = loadIcon(data, "nextIcon", "next");

        // Load the color and the small icon
        int color = data.hasKey("color") ? data.getInt("color") : NotificationCompat.COLOR_DEFAULT;
        int icon = data.hasKey("icon") ? Utils.getLocalResourceId(context, data.getMap("icon")) : 0;

        // Update properties
        nb.setColor(color);
        nb.setLights(color, 250, 250);
        nb.setSmallIcon(icon != 0 ? icon : playIcon);

        // Update the notification
        update();
    }

    public void updateMetadata(MediaMetadataCompat metadata) {
        MediaDescriptionCompat description = metadata.getDescription();

        // Fill notification info
        nb.setContentTitle(description.getTitle());
        nb.setContentText(description.getSubtitle());
        nb.setSubText(description.getDescription());
        nb.setLargeIcon(description.getIconBitmap());

        // Update the notification
        update();
    }

    public void updatePlayback(PlaybackStateCompat playback) {
        // Check and update the state
        boolean playing = Utils.isPlaying(playback.getState());
        nb.setOngoing(playing);

        // Check and update action buttons
        long actions = playback.getActions();
        play = updateAction(play, actions, PlaybackStateCompat.ACTION_PLAY, "Play", playIcon);
        pause = updateAction(pause, actions, PlaybackStateCompat.ACTION_PAUSE, "Pause", pauseIcon);
        stop = updateAction(stop, actions, PlaybackStateCompat.ACTION_STOP, "Stop", stopIcon);
        previous = updateAction(previous, actions, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS, "Previous", previousIcon);
        next = updateAction(next, actions, PlaybackStateCompat.ACTION_SKIP_TO_NEXT, "Next", nextIcon);

        // Add the action buttons
        nb.mActions.clear();
        if(previous != null) nb.mActions.add(0, previous);
        if(play != null && !playing) nb.mActions.add(1, play);
        if(pause != null && playing) nb.mActions.add(1, pause);
        if(stop != null) nb.mActions.add(2, stop);
        if(next != null) nb.mActions.add(3, next);

        // Add the play/pause button to the compact view
        if(nb.mActions.get(1) != null) {
            style.setShowActionsInCompactView(1);
        } else {
            style.setShowActionsInCompactView();
        }
        nb.setStyle(style);

        // Update the notification
        update();
    }

    private int loadIcon(ReadableMap data, String key, String iconName) {
        if(data.hasKey(key)) {
            // Load icon from option value
            int id = Utils.getLocalResourceId(context, data.getMap(key));
            if(id != 0) return id;
        }

        // Load default icon
        Resources r = context.getResources();
        String packageName = context.getPackageName();
        return r.getIdentifier(iconName, "drawable", packageName);
    }

    private Action updateAction(Action instance, long mask, long action, String title, int icon) {
        // The action is disabled, we'll not create it
        if((mask & action) == 0) return null;

        // The action is already created
        if(instance != null) return instance;

        // Create the action
        return new Action(icon, title, createActionIntent(action));
    }

    /**
     * Should be replaced by MediaButtonReceiver.buildMediaButtonPendingIntent
     * when React Native updates to a newer support library version
     */
    private PendingIntent createActionIntent(long action) {
        // Create an intent for the service
        Intent intent = new Intent(context, PlayerService.class);
        intent.putExtra(PlayerService.MEDIA_BUTTON, action);
        return PendingIntent.getService(context, 0, intent, 0);
    }

    private void update() {
        // Update the notification if it's showing
        if(showing) setActive(true);
    }

    public void setActive(boolean active) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);

        if(active) {
            manager.notify("TrackPlayer", 0, nb.build());
        } else {
            manager.cancel("TrackPlayer", 0);
        }
        showing = active;
    }

}