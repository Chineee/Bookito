package com.zerobudget.bookito.ui.library;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.zerobudget.bookito.R;
import com.zerobudget.bookito.databinding.FragmentLibraryBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding binding;
    private ArrayList<BookModel> bookModels;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public void setUpBookModel(){
        bookModels = new ArrayList<>();

        db.collection("users").document("lcEOKGRTqiyx6UgExmgD").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<BookModel> arrBkm = new ArrayList<>();

                        Object arr = task.getResult().get("books"); //array dei books
                        Iterator<Object> iterator = ((ArrayList<Object>) arr).iterator(); //cast ad array list per avere l'iteratore

                        int i = 0;//contatore
                        while (iterator.hasNext()) {
                            Object o = ((ArrayList<Object>) arr).get(i); //cast ad array list per prendere il libro i
                            HashMap<Object, Object> map = (HashMap<Object, Object>) o; // cast per prendere i dati del libro i
                            Log.d("AAA", "" + map.get("title"));

                            BookModel tmp = new BookModel((String) map.get("thumbnail"), (String) map.get("title"), null);
                            arrBkm.add(tmp);//aggiunge il bookmodel tmp all'array list

                            iterator.next();
                            i++;
                        }

                        addBookOnLibrary(arrBkm); //visualizza il libro nella libreria
                    }
                });
    }

     protected void addBookOnLibrary(ArrayList<BookModel> arr){
        RecyclerView recyclerView = binding.recycleViewMyLibrary;

        Book_RecycleViewAdapter adapter = new Book_RecycleViewAdapter(this.getContext(), arr);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this.getContext(), 2));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // LibraryViewModel libraryViewModel = new ViewModelProvider(this).get(LibraryViewModel.class);

        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setUpBookModel();
        //visulizzazione spostata in addBookOnLibrary()

        binding.floatingActionButton.setOnClickListener(view -> {
            Navigation.findNavController(view).navigate(R.id.action_navigation_library_to_navigation_insertNew);
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}