package com.zerobudget.bookito.ui.library;

import android.content.Context;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import com.zerobudget.bookito.R;
import com.zerobudget.bookito.models.book.BookModel;
import com.zerobudget.bookito.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.picasso.transformations.BlurTransformation;

public class Book_RecycleViewAdapter extends RecyclerView.Adapter<Book_RecycleViewAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<BookModel> bookModels;

    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;

    private final TextView emptyWarning;

    FirebaseFirestore db;
    FirebaseAuth auth;

    public Book_RecycleViewAdapter(Context context, ArrayList<BookModel> bookModels, TextView emptyWarning) {
        this.context = context;
        this.bookModels = bookModels;
        this.emptyWarning = emptyWarning;

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public Book_RecycleViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.recycleview_library, parent, false);

        return new Book_RecycleViewAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Book_RecycleViewAdapter.ViewHolder holder, int position) {
        holder.title.setText(bookModels.get(position).getTitle());

        //int book_number = holder.getAdapterPosition()+1;
        //holder.book_id.setText(book_number+"");
        if (bookModels.get(holder.getAdapterPosition()).getStatus()) {
            holder.wait_icon.setVisibility(View.GONE);
            //cambia dinamicamente i colori del bookmark sulla base dell'immagine di copertina del libro
            /*Picasso.get().load(bookModels.get(holder.getAdapterPosition()).getThumbnail()).into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    assert holder.thumbnail != null;
                    holder.thumbnail.setImageBitmap(bitmap);
                    Palette.from(bitmap)
                            .generate(palette -> {
                                assert palette != null;
                                Palette.Swatch textSwatch = palette.getMutedSwatch();
                                Palette.Swatch textSwatch2 = palette.getDarkMutedSwatch();

                                if (textSwatch == null)
                                    return;
                                if (textSwatch2 == null)
                                    return;

                                holder.bookmark_outline.setColorFilter(textSwatch2.getRgb(), PorterDuff.Mode.SRC_ATOP);
                                holder.bookmark.setColorFilter(textSwatch.getRgb(), PorterDuff.Mode.SRC_ATOP);
                                holder.book_id.setTextColor(textSwatch.getBodyTextColor());
                            });
                }
                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {}

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {}


            });*/
            Picasso.get().load(bookModels.get(position).getThumbnail()).into(holder.thumbnail);
        } else {
            Picasso.get().load(bookModels.get(position).getThumbnail()).transform(new BlurTransformation(context, 3, 2)).into(holder.thumbnail);

            //cambia dinamicamente i colori del bookmark sulla base dell'immagine di copertina del libro
            /*Picasso.get().load(bookModels.get(holder.getAdapterPosition()).getThumbnail()).transform(new BlurTransformation(context, 3, 2)).into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    assert holder.thumbnail != null;
                    holder.thumbnail.setImageBitmap(bitmap);
                    Palette.from(bitmap)
                            .generate(palette -> {
                                assert palette != null;
                                Palette.Swatch textSwatch = palette.getMutedSwatch();
                                Palette.Swatch textSwatch2 = palette.getDarkMutedSwatch();

                                if (textSwatch == null)
                                    return;
                                if (textSwatch2 == null)
                                    return;

                                holder.bookmark_outline.setColorFilter(textSwatch2.getRgb(), PorterDuff.Mode.SRC_ATOP);
                                holder.bookmark.setColorFilter(textSwatch.getRgb(), PorterDuff.Mode.SRC_ATOP);
                            });
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {}

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {}
            });*/

            holder.wait_icon.setVisibility(View.VISIBLE);
        }

        holder.author.setText(bookModels.get(position).getAuthor());

        loadIconBookType(holder, holder.book_type);

        holder.book_selected.setOnClickListener(view -> {
            createNewDeletePopup(holder);
        });
    }

    /**
     * popup creato qunado l'utente seleziona un libro
     * permette di visualizzare le informazioni relative ad esso
     * permette di eliminare il libro se esso è abilitato (status = true)*/
    private void createNewDeletePopup(ViewHolder holder) {
        dialogBuilder = new MaterialAlertDialogBuilder(context);
        View view = View.inflate(context, R.layout.popup_delete_book, null);

        TextView bookTitle = view.findViewById(R.id.book_title);
        TextView bookAuthor = view.findViewById(R.id.book_author);
        TextView bookDescription = view.findViewById(R.id.book_description);
        Button btnDelete = view.findViewById(R.id.btn_delete);
        ImageView bookThumbnail = view.findViewById(R.id.book_thumbnail);
        ImageView bookType = view.findViewById(R.id.icon_type);

        bookTitle.setText(bookModels.get(holder.getAdapterPosition()).getTitle());
        bookAuthor.setText(bookModels.get(holder.getAdapterPosition()).getAuthor());
        bookDescription.setText(bookModels.get(holder.getAdapterPosition()).getDescription());
        bookDescription.setMovementMethod(new ScrollingMovementMethod());
        Picasso.get().load(bookModels.get(holder.getAdapterPosition()).getThumbnail()).into(bookThumbnail);
        loadIconBookType(holder, bookType);


        //se il libro è in una richiesta accettata non può essere eliminato
        if (!bookModels.get(holder.getAdapterPosition()).getStatus()) {
            btnDelete.setEnabled(false);
        }else{
            bookType.setOnClickListener(view12 -> {
                Toast.makeText(context, "TODO: implementare il cambio di tipo", Toast.LENGTH_LONG).show();
            });
        }

        btnDelete.setOnClickListener(view1 -> {
            //conferma dell'eliminazione
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle("Conferma eliminazione");
            builder.setMessage(Html.fromHtml("Sei sicuro di voler eliminare il libro: <br><b>" + bookModels.get(holder.getAdapterPosition()).getTitle() + "</b>?", Html.FROM_HTML_MODE_LEGACY));
            builder.setPositiveButton("SI", (dialogInterface, i) -> {
                Log.d("ELIMINO:", "" + bookModels.get(holder.getAdapterPosition()));
                //rimuove il libro selezionato
                db.collection("users").document(Utils.USER_ID).update("books", FieldValue.arrayRemove(bookModels.get(holder.getAdapterPosition())));

                //rimuove la richiesta relativa a quel libro se esiste ed è undefined
                db.collection("requests").whereEqualTo("status", "undefined").whereEqualTo("receiver", Utils.USER_ID).whereEqualTo("requestedBook", bookModels.get(holder.getAdapterPosition())).get().addOnCompleteListener(task -> {
                    List<DocumentSnapshot> documents = task.getResult().getDocuments();
                    for (DocumentSnapshot document : documents) {
                        DocumentReference documentReference = document.getReference();
                        documentReference.delete();
                    }
                });

                Toast.makeText(context, bookModels.get(holder.getAdapterPosition()).getTitle() + " eliminato!", Toast.LENGTH_LONG).show();
                bookModels.remove(holder.getAdapterPosition());
                notifyItemRemoved(holder.getAdapterPosition());
                Utils.toggleEmptyWarning(emptyWarning, Utils.CURRENT_USER.getLibrary().size());
                dialogInterface.dismiss();
            }).setNegativeButton("NO", (dialogInterface, i) -> {
                dialogInterface.dismiss();
            }).show();

            dialog.dismiss();
        });

        dialogBuilder.setView(view);
        dialog = dialogBuilder.create();
        dialog.show();
    }

    private void loadIconBookType(ViewHolder holder, ImageView icon){
        switch (bookModels.get(holder.getAdapterPosition()).getType()) {
            case "Scambio":
                Picasso.get().load(R.drawable.swap).into(icon);
                break;
            case "Prestito":
                Picasso.get().load(R.drawable.calendar).into(icon);
                break;
            case "Regalo":
                Picasso.get().load(R.drawable.gift).into(icon);
                break;
            default:
                break;
        }
    }

    @Override
    public int getItemCount() {
        return bookModels.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ConstraintLayout book_selected;
        private final ImageView thumbnail;
        private final TextView title;
        private final TextView author;
        private final TextView owner;
        //private final ImageView bookmark;
        //private final ImageView bookmark_outline;
        private final ImageView wait_icon;
        private final ImageView book_type;

        //private final TextView book_id;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.book_thumbnail);
            title = itemView.findViewById(R.id.book_title);
            author = itemView.findViewById(R.id.book_author);
            owner = itemView.findViewById(R.id.book_owner);
            book_selected = itemView.findViewById(R.id.book);
            //bookmark = itemView.findViewById(R.id.bookmark);
            //bookmark_outline = itemView.findViewById(R.id.bookmark_outline);
            wait_icon = itemView.findViewById(R.id.wait_icon);
            book_type = itemView.findViewById(R.id.icon_type);
            //book_id = itemView.findViewById(R.id.book_id);
        }
    }

}
