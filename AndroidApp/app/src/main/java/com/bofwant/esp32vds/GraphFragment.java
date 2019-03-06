package com.bofwant.esp32vds;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GraphFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GraphFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GraphFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    public MainActivity mainActivity;
    public Button updateButton;
    public GraphView graph;
    private LineGraphSeries<DataPoint> dSeries;
    private final Handler mHandler = new Handler();
    private Runnable gTimer;
    private boolean updating;
    private OnFragmentInteractionListener mListener;

    public GraphFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment GraphFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GraphFragment newInstance(String param1, String param2) {
        GraphFragment fragment = new GraphFragment();
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
        return inflater.inflate(R.layout.fragment_graph, container, false);
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
        for(int i=0;i<200000;i++){
            mainActivity.sampleBuffer[i]=0;
        }
        dSeries=new LineGraphSeries<>(generateDataPoints());
        graph=(GraphView) getView().findViewById(R.id.graph);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-1.5);
        graph.getViewport().setMaxY(1.5);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(1);

        // enable scaling and scrolling
        graph.getViewport().setScalable(true);
        //graph.getViewport().setScalableY(true);

        graph.addSeries(dSeries);
        updateButton=(Button)getView().findViewById(R.id.updateButton);
        updateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mainActivity=(MainActivity)getActivity();
                if(updating){
                    updating=false;
                    changeUpdateButton(false);
                    mHandler.removeCallbacks(gTimer);
                }else {
                    updating=true;
                    changeUpdateButton(true);
                    gTimer = new Runnable() {
                        @Override
                        public void run() {
                            updating=true;
                            dSeries.resetData(generateDataPoints());
                            mHandler.postDelayed(this, 100);
                        }
                    };
                    mHandler.postDelayed(gTimer, 100);
                }

            }
        });
    }
    public DataPoint[] generateDataPoints() {
        DataPoint[] values = new DataPoint[200000];
        for (int i=0; i<200000; i++) {
            values[i]= new DataPoint(i*0.000005, (2000-mainActivity.sampleBuffer[i])*0.00075);
        }
        return values;
    }
    public void changeUpdateButton(boolean updating){
        if(updating){
            ///connected
            updateButton.setText("Updating");
            //updateButton.setCompoundDrawablesWithIntrinsicBounds(getContext().getResources().getDrawable(R.drawable.ic_power_black_24dp),null,null,null);

        }else {
            ////disconnect
            updateButton.setText("Start Update");
            //updateButton.setCompoundDrawablesWithIntrinsicBounds(getContext().getResources().getDrawable(R.drawable.ic_baseline_power_off_24px),null,null,null);

        }
    }
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
