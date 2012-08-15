package org.openintents.wifiserver.requesthandler.notes;

import org.openintents.wifiserver.requesthandler.BasicAuthentiationHandler;

import android.content.Context;
import android.net.Uri;

/**
 * This is the base class for the notepad support. It just includes some
 * global attributes like the content provider's URI and the applications
 * context.
 *
 * @author Stanley Förster
 *
 */
public abstract class NotesHandler extends BasicAuthentiationHandler {

    protected final static String TAG = NotesHandler.class.getSimpleName();
    protected final static Uri mNotesURI = Uri.parse("content://org.openintents.notepad/notes");
    protected final Context mContext;

    /**
     * Creates a new handler.
     *
     * @param context The application's context.
     */
    public NotesHandler(Context context) {
        this.mContext = context;
    }
}
