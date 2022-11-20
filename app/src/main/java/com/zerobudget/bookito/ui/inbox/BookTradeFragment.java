package com.zerobudget.bookito.ui.inbox;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.zerobudget.bookito.databinding.FragmentTradeBookBinding;
import com.zerobudget.bookito.models.Requests.RequestTradeModel;
import com.zerobudget.bookito.models.book.BookModel;
import com.zerobudget.bookito.models.users.UserModel;
import com.zerobudget.bookito.ui.search.SearchResultsModel;
import com.zerobudget.bookito.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class BookTradeFragment extends Fragment {

    private FragmentTradeBookBinding binding;
    private RequestTradeModel requestTradeModel;
    private FirebaseFirestore db;

    private ArrayList<String> senderTradedBooks;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTradeBookBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        senderTradedBooks = new ArrayList<>();

        Bundle args = getArguments();
        assert args != null;
        String str = args.getString("BK");
        requestTradeModel = Utils.getGsonParser().fromJson(str, RequestTradeModel.class);

        Log.d("REQ", requestTradeModel.getSender());

        db = FirebaseFirestore.getInstance();


        getAllSenderTradedBooks();
        setUpBookModel();
        return root;
    }


    private void setUpBookModel() {
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ArrayList<SearchResultsModel> arrResults = new ArrayList<>(); //libri trovati

                for (DocumentSnapshot document : task.getResult()) {
                    if (document.getId().equals(requestTradeModel.getSender())) { //deve cercare i libri di chi ha fatto la richiesta
                        Object arr = document.get("books"); //array dei books
                        if (arr != null) { //si assicura di cercare solo se esiste quache libro
                            for (Object o : (ArrayList<Object>) arr) {
                                HashMap<Object, Object> map = (HashMap<Object, Object>) o;
                                //mostra solo i libri che non sono già in una qualche richiesta di scambio!
                                if (map.get("type").equals("Scambio") && !senderTradedBooks.contains(map.get("isbn"))) {
                                    BookModel tmp = new BookModel((String) map.get("thumbnail"), (String) map.get("isbn"), (String) map.get("title"), (String) map.get("author"), (String) map.get("description"), (String) map.get("type"));
                                    SearchResultsModel searchResultsModel = new SearchResultsModel(tmp, UserModel.getUserFromDocument(document));
                                    arrResults.add(searchResultsModel);
                                }
                            }
                        }
                    }
                }

                if(arrResults.size() > 0) {
                    binding.noBooksFound.setVisibility(View.GONE);
                    viewBooks(arrResults);
                }else{
                    //nessun libro disponibile
                    binding.noBooksFound.setVisibility(View.VISIBLE);
                }
            } else {
                Log.d("TAG", "Error getting documents: ", task.getException());
            }

        });
    }

    private void getAllSenderTradedBooks() {
        db.collection("requests").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    if(doc.contains("requestTradeBook")) {
                        //Log.d("AA", (String) doc.get("requestTradeBook"));
                        if(doc.get("type").equals("Scambio") && ((doc.get("sender").equals(requestTradeModel.getSender()) || doc.get("receiver").equals(requestTradeModel.getSender())))){
                            senderTradedBooks.add((String) doc.get("requestTradeBook"));
                           Log.d("REQ", (String) doc.get("requestTradeBook"));
                        }
                    }
                }
            } else {
                Log.w("ERR", "Error getting documents.", task.getException());
            }
        });
    }




    protected void viewBooks(ArrayList<SearchResultsModel> arr) {
        if (getView() != null) { //evita il crash dell'applicazione
            RecyclerView recyclerView = binding.recycleViewMyLibrary;

            BookTrade_RecycleViewAdapter adapter = new BookTrade_RecycleViewAdapter(this.getContext(), arr, requestTradeModel);

            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new GridLayoutManager(this.getContext(), 2));
        }
    }
}