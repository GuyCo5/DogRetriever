package dtg.dogretriever.Model;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.Map;

import dtg.dogretriever.Presenter.MainActivity;

public class FirebaseAdapter {
    //singleton
    private static FirebaseAdapter instance;

    //Firebase refrences
    FirebaseDatabase dataBase;
    DatabaseReference dogTableRef;
    DatabaseReference usersTableRef;
    FirebaseAuth mAuth;


    ArrayList<Dog> DogsFromDataBaseList;
    ArrayList<Profile> ProfilesFromDataBaseList;
    Profile profile;
    FirebaseAuth.AuthStateListener mAuthListener;
    String userID;
    final DataSnapshot[] mainDataSnapshot= new DataSnapshot[1];

    private FirebaseAdapter() {

        //Init FireBase
        dataBase = FirebaseDatabase.getInstance();

        //Referenecs for both tables
        dogTableRef = dataBase.getReference().child("Dogs");
        usersTableRef = dataBase.getReference().child("Users");

        DogsFromDataBaseList = new ArrayList<Dog>(); //This is where the list of dog will be after it get it from firebase
        ProfilesFromDataBaseList = new ArrayList<Profile>(); //This is where the list of profiles will be after it get it from firebase
        profile = new Profile();
        mAuth = FirebaseAuth.getInstance();

        //Firebase Listeners
/*
        if(mAuth.getCurrentUser() != null){
            userID = mAuth.getCurrentUser().getUid();
        }
*/
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(mAuth.getCurrentUser() != null){
                    userID = mAuth.getCurrentUser().getUid();

                }
            }


        };

        dogTableRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // This event happens when app start and when something change on the dog table
                DogsFromDataBaseList.clear();
                Log.e("Count ", "" + dataSnapshot.getChildrenCount());

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Dog dog = postSnapshot.getValue(Dog.class);
                    Log.d("onChildAdded", "dog:" + dog.getName());
                    DogsFromDataBaseList.add(dog);
                }

            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });






          //  usersTableRef.orderByKey().equalTo(mAuth.getUid()).addValueEventListener(new ValueEventListener() {
            usersTableRef.addValueEventListener(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                   // ProfilesFromDataBaseList.clear();
                    //profile = dataSnapshot.getValue(Profile.class);
                    mainDataSnapshot[0] = dataSnapshot;

             //       for (DataSnapshot temp: dataSnapshot.getChildren()){
                    /*
                        if(dataSnapshot.child(userID).hasChild("id"));
                            profile.setId((String)dataSnapshot.child(userID).child("id").getValue());
                        if(dataSnapshot.child(userID).hasChild("fullName"))
                            profile.setFullName((String)dataSnapshot.child(userID).child("fullName").getValue());
                        if(dataSnapshot.child(userID).hasChild("phoneNumber"))
                            profile.setPhoneNumber((String)dataSnapshot.child(userID).child("phoneNumber").getValue());
                        if(dataSnapshot.child(userID).hasChild("address"))
                            profile.setAddress((String)dataSnapshot.child(userID).child("address").getValue());
                        if(dataSnapshot.child(userID).hasChild("eMail"))
                            profile.seteMail((String)dataSnapshot.child(userID).child("eMail").getValue());
                        if(dataSnapshot.child(userID).hasChild("dogsIDArrayList"))
                            profile.setDogsIDMap((Map<String,String>) dataSnapshot.child(userID).child("dogsIDArrayList").getValue());
                 */
                   // }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });


    }

    public static FirebaseAdapter getInstanceOfFireBaseAdapter(){
        if(instance == null){
            instance = new FirebaseAdapter();
        }
        return instance;
    }

    public void addUserToDataBase(Profile user){
        //Gets user Profile and save it to FireBase by ID
        //This method would overwrite database child if the user have the same id
        usersTableRef.child(user.getId()).setValue(user);
    }

    public void addDogToDataBase(Dog dog){
        //Gets dog and save it to FireBase by ID
        //This method would overwrite database child if the user have the same id
        //This methood will add the dogId to the current user profile

        DatabaseReference currentUserRef = usersTableRef.child(mAuth.getUid()).child("dogsIDArrayList");
        String hashCode = currentUserRef.push().getKey();

        currentUserRef.child(hashCode).setValue(dog.getCollarId());

        dog.setHashCode(hashCode);

        dogTableRef.child(dog.getCollarId()+"").setValue(dog);

    }

    public void removeDogFromDataBase(Dog dog){
        //Delete the dogs and it's scans from database
        dogTableRef.child(dog.getCollarId()).removeValue();

        //Delete the dog Id from the user profile
        DatabaseReference currentUserRef = usersTableRef.child(mAuth.getUid()).child("dogsIDArrayList").child(dog.getHashCode());
        currentUserRef.removeValue();
    }

    public Dog getDogByCollarIdFromFireBase(String collarId){
     //return dog from database by id if dog not found return null

        for (Dog dog : DogsFromDataBaseList){
            if(dog.getCollarId().equals(collarId)){
                return dog;
            }
        }
        return null;
    }

    public void addScanToDog(Dog dog,Scan scan){
        //Get dog and scan and add the scan to the dog scan map at firebase
        DatabaseReference currentDogRef = dogTableRef.child(dog.getCollarId()).child("scans");
        String hashCode = currentDogRef.push().getKey();

        currentDogRef.child(hashCode).setValue(scan);
    }

    public Map<String,Scan> getAllScanOfSpecificDog(Dog dog){
        //Get dog and give back all his scans as map
        return getDogByCollarIdFromFireBase(dog.getCollarId()).getScans();
    }

    public boolean isUserConnected(){
        //check if user is connected right now to firebase
        if(mAuth.getCurrentUser()!=null){
            return true;
        }
        else{
            return false;
        }
    }

    public Profile getCurrentUserProfileFromFireBase(){
        userID = mAuth.getCurrentUser().getUid();

        if(mainDataSnapshot[0].child(userID).hasChild("id"));
        profile.setId((String)mainDataSnapshot[0].child(userID).child("id").getValue());
        if(mainDataSnapshot[0].child(userID).hasChild("fullName"))
            profile.setFullName((String)mainDataSnapshot[0].child(userID).child("fullName").getValue());
        if(mainDataSnapshot[0].child(userID).hasChild("phoneNumber"))
            profile.setPhoneNumber((String)mainDataSnapshot[0].child(userID).child("phoneNumber").getValue());
        if(mainDataSnapshot[0].child(userID).hasChild("address"))
            profile.setAddress((String)mainDataSnapshot[0].child(userID).child("address").getValue());
        if(mainDataSnapshot[0].child(userID).hasChild("eMail"))
            profile.seteMail((String)mainDataSnapshot[0].child(userID).child("eMail").getValue());
        if(mainDataSnapshot[0].child(userID).hasChild("dogsIDArrayList"))
            profile.setDogsIDMap((Map<String,String>) mainDataSnapshot[0].child(userID).child("dogsIDArrayList").getValue());



        return profile;
    }

    public ArrayList<Dog> getListOfDogsOwnedByCurrentUser(){
        //give an Arraylist of dogs owned by the current user
        //if no dogs found will return null

        ArrayList<Dog> dogArrayList = new ArrayList<>();

        try {
            for (String dogId : profile.getDogIDAsArrayList()) {
                dogArrayList.add(getDogByCollarIdFromFireBase(dogId));
            }
        }catch (NullPointerException e){
            return null;
        }
        return  dogArrayList;
    }

    public void writeNewTokenToFireBase(String token) {
            DatabaseReference currentUserRef = usersTableRef.child(mAuth.getUid()).child("token");
            currentUserRef.setValue(token);
    }



}