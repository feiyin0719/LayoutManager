package com.iffly.layoutmanager;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CustomGridLayoutManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.PhantomReference;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MyAdapter adapter;
    private CustomGridLayoutManager gridLayoutManager;
    private ScaleGestureDetector.OnScaleGestureListener onScaleGestureListener=new ScaleGestureDetector.OnScaleGestureListener() {
        private float prev;
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Log.d("test","factor:"+detector.getScaleFactor());
            if(detector.getScaleFactor()>1){
                prev=detector.getScaleFactor();
                gridLayoutManager.incChangeProcess();

            }else if(detector.getScaleFactor()<1){

                prev=detector.getScaleFactor();
                gridLayoutManager.reduceChangeProcess();
            }
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            Log.d("test","scale start");
            gridLayoutManager.startSpanChange();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            Log.d("test","scale end");
            gridLayoutManager.stopSpanChange();
        }
    };
    private ScaleGestureDetector scaleGestureDetector;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView=findViewById(R.id.recyclerView);
        gridLayoutManager=new CustomGridLayoutManager(4);
        adapter=new MyAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(gridLayoutManager);
        scaleGestureDetector=new ScaleGestureDetector(this,onScaleGestureListener);
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event);
                return false;
            }
        });
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(){
                    public void run(){
                        int i=0;
                        gridLayoutManager.startSpanChange();
                        while(i<=24){
                            ++i;
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            final int finalI = i;
                            recyclerView.post(new Runnable() {
                                @Override
                                public void run() {
                                    gridLayoutManager.incChangeProcess();
                                }
                            });
                        }
                        recyclerView.post(new Runnable() {
                            @Override
                            public void run() {
                                gridLayoutManager.stopSpanChange();
                            }
                        });


                    }


                }.start();
            }
        });
        //LinearLayoutManager

    }
    private static class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder>{

        private Context context;

        public MyAdapter(Context context) {
            this.context = context;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view= LayoutInflater.from(context).inflate(R.layout.item_layout,viewGroup,false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {
            TextView textView=myViewHolder.itemView.findViewById(R.id.textView);
            textView.setText(""+i);
        }

        @Override
        public int getItemCount() {
            return 100;
        }

        public static class MyViewHolder extends RecyclerView.ViewHolder{
            public MyViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }

    }
}
