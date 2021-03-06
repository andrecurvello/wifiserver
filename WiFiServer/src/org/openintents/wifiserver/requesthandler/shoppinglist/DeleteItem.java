package org.openintents.wifiserver.requesthandler.shoppinglist;

import static android.provider.BaseColumns._ID;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.openintents.shopping.library.provider.ShoppingContract.Items;
import org.openintents.shopping.library.provider.ShoppingContract.Lists;
import org.openintents.shopping.library.util.ShoppingUtils;
import org.openintents.wifiserver.util.URLUtil;

import android.content.Context;
import android.database.Cursor;

/**
 * Handler which is used to delete items. It handles requests of the form "/shoppinglist/item/delete".
 *
 * @author Stanley Förster
 *
 */
public class DeleteItem extends ShoppinglistHandler {

    /**
     * Creates a new handler.
     *
     * @param context The application's context.
     */
    public DeleteItem(Context context) {
        super(context);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * This method deletes an item. The required HTTP method is GET.
     * Every other method will cause a 405 status code to be returned.
     * If no parameter is given, all items will be deleted.
     * To specify a special item, the <code>id</code> and <code>list</code> parameters can be used:
     * <ul>
     * <li><code>id</code> specifies the item, that should be deleted. It will be removed from all lists and from the app's database.</li>
     * <li><code>list</code> specifies the list from which the item should be deleted. If only the list parameter is given, the whole list will be cleared.</li>
     * </ul>
     */
    @Override
    public void getResponse(HttpRequest request, HttpResponse response, HttpContext context) {
        if (!"GET".equals(request.getRequestLine().getMethod())) {
            response.setStatusCode(405);
            return;
        }

        String id = URLUtil.getParameter(request.getRequestLine().getUri(), "id");
        String list = URLUtil.getParameter(request.getRequestLine().getUri(), "list");

        if (id != null && list != null) {
            ShoppingUtils.deleteItem(mContext, id, list);
            response.setStatusCode(200);
            return;
        }

        if (id != null && list == null) {
            Cursor listsCursor = mContext.getContentResolver().query(Lists.CONTENT_URI, new String[] { _ID }, null, null, null);

            if (listsCursor == null)
                return;

            if (listsCursor.moveToFirst())
                do {
                    long listId = listsCursor.getLong(listsCursor.getColumnIndex(Lists._ID));
                    ShoppingUtils.deleteItem(mContext, id, listId+"");
                } while (listsCursor.moveToNext());

            listsCursor.close();
        }

        if (id == null && list != null) {
            clearList(list);
        }

        if (id == null && list == null) {
            Cursor listsCursor = mContext.getContentResolver().query(Lists.CONTENT_URI, new String[] { _ID }, null, null, null);

            if (listsCursor == null)
                return;

            if (listsCursor.moveToFirst())
                do {
                    long listId = listsCursor.getLong(listsCursor.getColumnIndex(Lists._ID));
                    clearList(listId+"");
                } while (listsCursor.moveToNext());

            listsCursor.close();
        }
    }

    /**
     * Clears the list with the given id by removing all items from that list.
     *
     * @param listId
     *            The list that should be cleared.
     */
    private void clearList(String listId) {

        Cursor itemCursor = mContext.getContentResolver().query(Items.CONTENT_URI, new String[] { _ID }, null, null, null);

        if (itemCursor == null)
            return;

        if (itemCursor.moveToFirst())
            do {
                long itemId = itemCursor.getLong(itemCursor.getColumnIndex(Items._ID));
                ShoppingUtils.deleteItem(mContext, itemId+"", listId);
            } while (itemCursor.moveToNext());

        itemCursor.close();
    }
}
