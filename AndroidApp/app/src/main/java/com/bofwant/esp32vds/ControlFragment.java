package com.bofwant.esp32vds;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ControlFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ControlFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ControlFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    public MainActivity mainActivity;
    public ImageButton upButton,downButton;
    public SeekBar potBar,muxBar;
    public TextView potTextView,muxTextView;
    private OnFragmentInteractionListener mListener;

    public ControlFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ControlFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ControlFragment newInstance(String param1, String param2) {
        ControlFragment fragment = new ControlFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_control, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    @Override
    public void onStart() {
        super.onStart();
        mainActivity=(MainActivity)getActivity();

        upButton=(ImageButton) getView().findViewById(R.id.upButton);
        downButton=(ImageButton) getView().findViewById(R.id.downButton);
        potBar=(SeekBar) getView().findViewById(R.id.potBar);
        muxBar=(SeekBar) getView().findViewById(R.id.muxBar);
        potTextView=(TextView)getView().findViewById(R.id.potTextView);
        muxTextView=(TextView)getView().findViewById(R.id.muxTextView);

        upButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mainActivity=(MainActivity)getActivity();
                mainActivity.txQueue.add(getActivity().getResources().getString(R.string.esp_potup));
            }
        });
        downButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mainActivity=(MainActivity)getActivity();
                mainActivity.txQueue.add(getActivity().getResources().getString(R.string.esp_potdown));
            }
        });
        potBar.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
            int pot = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
            pot=progresValue;
            potTextView.setText("128--"+String.valueOf(pot));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mainActivity.txQueue.offer(getActivity().getResources().getString(R.string.esp_pot)+" "+String.valueOf(pot));
                mainActivity.potValue=pot;
                Toast.makeText(getActivity(), "Wiper "+String.valueOf(pot), Toast.LENGTH_SHORT).show();

            }
        });
        muxBar.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
            int muxCh = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                muxCh = progresValue;
                muxTextView.setText("Canal 0-3 -- "+String.valueOf(muxCh));
                }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mainActivity.txQueue.offer(getActivity().getResources().getString(R.string.esp_mux)+" "+String.valueOf(muxCh));
                mainActivity.muxChanel=muxCh;
                Toast.makeText(getActivity(), "Mux Chanel "+String.valueOf(muxCh), Toast.LENGTH_SHORT).show();

            }
        });
    }
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
