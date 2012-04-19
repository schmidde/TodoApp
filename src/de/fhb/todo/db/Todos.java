package de.fhb.todo.db;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Convenience definitions for NotePadProvider
 */
public final class Todos {
    public static final String AUTHORITY = "de.fhb.todo.provider.ToDo";

    // This class cannot be instantiated
    private Todos() {}

    /**
     * Todo table
     */
    public static final class TodoColumns implements BaseColumns {
        // This class cannot be instantiated
        private TodoColumns() {}

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/tasks");

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single task.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.de.fhb.todo";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "isDone ASC";

        /**
         * Die Sortierung erfolt erst nach Datum dann nach Wichtigkeit
         */
        public static final String SORT_ORDER_DATE_FAVORITE = "isDone ASC, created DESC, isFavourite DESC";
        
        /**
         * Die Sortierung erfolgt erst nach Wichtigkeit und dann nach Datum
         */
        public static final String SORT_ORDER_FAVORITE_DATE = "isDone ASC, isFavourite DESC, created DESC";

        /**
         * The title of the task
         * <P>Type: TEXT</P>
         */
        public static final String TITLE = "title";

        /**
         * The task description
         * <P>Type: TEXT</P>
         */
        public static final String DESCRIPTION = "description";

        /**
         * The timestamp for the finishDate of the task
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String FINISH_DATE = "created";

        /**
         * The flag which indicates if the task is done
         * <P>Type: BOOLEAN</P>
         */
        public static final String DONE = "isDone";

        /**
         * The flag which indicates if the task is a favourite
         * <P>Type: BOOLEAN</P>
         */
        public static final String FAVOURITE = "isFavourite";

        /**
         * The id of the user from the webserver
         * <P>Type: INTEGER</P>
         */
        //public static final String USER_ID = "userId";

        public static final String LONGITUDE = "longitude";

        public static final String LATITUDE = "latitude";
        
        public static final String IS_SYNCED = "isSynced";
        
        public static final String SERVER_TASK_ID = "serverTaskId";
    }
    
    /**
     * Todo table
     */
    public static final class TodoContactColumns implements BaseColumns {
        // This class cannot be instantiated
        private TodoContactColumns() {}

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/contacts");

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single task.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.de.fhb.todo";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "_id DESC";

        /**
         * The taskId to which this contact is assigned
         * <P>Type: INTEGER</P>
         */
        public static final String TASK_ID = "task_id";

        /**
         * The Uri of this contact, so that we can do a lookup in the contact provider
         * <P>Type: STRING</P>
         */
        public static final String CONTACT_URI = "contact_uri";
    }
}
