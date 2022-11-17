package com.zerobudget.bookito.ui.add;

import static com.zerobudget.bookito.utils.Utils.isAValidISBN;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.zerobudget.bookito.R;
import com.zerobudget.bookito.databinding.FragmentAddBinding;
import com.zerobudget.bookito.models.book.BookModel;
import com.zerobudget.bookito.models.users.UserModel;
import com.zerobudget.bookito.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AddFragment extends Fragment {

    private FragmentAddBinding binding;
    private RequestQueue mRequestQueue;
    private BookModel newBook;
    private View root;

    private FirebaseFirestore db;

    /**
     * interazione con l'api di google books per la ricerca del libro tramite isbn scannerizzato*/
    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) { //isbn scannerizzato

            searchBookAPI(result.getContents());

        }
    });

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.newBook = new BookModel();

        db = FirebaseFirestore.getInstance();

        binding = FragmentAddBinding.inflate(inflater, container, false);
        root = binding.getRoot();


        //TextView textView = binding.textNotifications;
        //addViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        //addViewModel.getScore().observe(getViewLifecycleOwner(), textView::setText);

       /* binding.addOneBtm.setOnClickListener(v -> {
            addViewModel.plusScore();
        });

        binding.subOneBtn.setOnClickListener(view -> {
            addViewModel.subScore();
            // Toast.makeText(getActivity().getApplicationContext(), newBook.getTitle(), Toast.LENGTH_SHORT).show();
        });*/

        binding.scanBtn.setOnClickListener(view -> {
            ScanOptions options = new ScanOptions();
            options.setBeepEnabled(false);
            options.setPrompt("Premi 'volume su' per accendere la torcia");
            options.setOrientationLocked(true);
            options.setCaptureActivity(CaptureAct.class);

            barLauncher.launch(options);
        });

        binding.isbnNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                //si assicura che vegnano inserti esattamente 13 caratteri

                if(editable.toString().length() < 13) {
                    binding.btnAdd.setEnabled(false);
                }
                if(editable.toString().length() == 13)
                    binding.btnAdd.setEnabled(true);
            }
        });

        binding.btnAdd.setOnClickListener(view -> {
            String isbn = binding.isbnNumber.getText().toString();


            if(isAValidISBN(Long.parseLong(isbn)))
                searchBookAPI(isbn);
            else {
                AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this.getContext());
                builder.setTitle("Attenzione");
                builder.setMessage("L'isbn inserito non è valido, si prega di riprovare");
                builder.setPositiveButton("OK", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                }).show();
                binding.isbnNumber.setError("Attezione! L'isbn deve avere 13 caratteri");
                binding.isbnNumber.requestFocus();
            }
        });

        Log.d("USER NOW", ""+UserModel.getCurrentUser().serialize());
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    static public class CaptureAct extends CaptureActivity {

    }

    private void searchBookAPI(String isbn){
        mRequestQueue = Volley.newRequestQueue(getActivity().getApplicationContext());
        mRequestQueue.getCache().clear();
        // url per cercare il libro in base all'ISBN scannerizzato
        String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;

        // below line we are  creating a new request queue.
        RequestQueue queue = Volley.newRequestQueue(getActivity().getApplicationContext());

        JsonObjectRequest booksObjrequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            try {
                //prova a prlevare i dati in risposta dall'API di goole books
                JSONArray itemsArray = response.getJSONArray("items");
                JSONObject itemsObj = itemsArray.getJSONObject(0);
                JSONObject volumeObj = itemsObj.getJSONObject("volumeInfo");

                //riempe newBook con i dati prelevati
                newBook.setIsbn(isbn);
                newBook.setTitle(volumeObj.optString("title"));

                JSONArray authorsArray = volumeObj.getJSONArray("authors");

                String description = volumeObj.optString("description");
                if(!description.equals(""))
                    newBook.setDescription(description);
                else
                    newBook.setDescription("No description found");

                //data we might need in the future!

                //String subtitle = volumeObj.optString("subtitle");
                //String publisher = volumeObj.optString("publisher");
                //String publishedDate = volumeObj.optString("publishedDate");
                //String previewLink = volumeObj.optString("previewLink");
                //String infoLink = volumeObj.optString("infoLink");
                //int pageCount = volumeObj.optInt("pageCount");
                //JSONObject saleInfoObj = itemsObj.optJSONObject("saleInfo");
                //String buyLink = saleInfoObj.optString("buyLink");

                JSONObject imageLinks = volumeObj.optJSONObject("imageLinks");


                if (imageLinks != null) {
                    //l'API rende un link che inizia con http ma Picasso, usato per estrarre l'immagine ha bisogno dell'https
                    newBook.setThumbnail("https".concat(imageLinks.optString("thumbnail").substring(4)));
                } else {
                    //se l'immagine non è disponibile ne viene usata una di default
                    newBook.setThumbnail("https://feb.kuleuven.be/drc/LEER/visiting-scholars-1/image-not-available.jpg/image");
                }

                //soltanto il primo autore viene utilizzato
                ArrayList<String> authorsArrayList = new ArrayList<>();
                if (authorsArray.length() != 0) {
                    for (int j = 0; j < authorsArray.length(); j++) {
                        authorsArrayList.add(authorsArray.optString(0));
                    }
                    newBook.setAuthor(authorsArrayList.get(0));
                } else
                    newBook.setAuthor(null);

                //passaggio dei dati del new book al prossimo fragment
                Bundle args = new Bundle();
                String bookString = Utils.getGsonParser().toJson(newBook);
                args.putString("BK", bookString);

                Navigation.findNavController(root).navigate(R.id.action_navigation_insertNew_to_addConfirmFragment, args);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> {
            //TODO
            Toast.makeText(this.getContext(), "Errore!", Toast.LENGTH_LONG);
        });
        queue.add(booksObjrequest);
    }


}