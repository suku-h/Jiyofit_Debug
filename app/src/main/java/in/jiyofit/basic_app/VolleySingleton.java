package in.jiyofit.basic_app;

import android.graphics.Bitmap;
import android.util.LruCache;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

/*
If your application makes constant use of the network, it's probably most efficient to set up
a single instance of RequestQueue that will last the lifetime of your app.
Volley used for queueing up requests, it makes no multiple queues. Hence volley uses singleton
the singleton pattern is a design pattern that restricts the instantiation of a class to one object.
Hence it is instantiated as public static assigned null value
*/
public class VolleySingleton {
    private static VolleySingleton sInstance = null;
    private RequestQueue requestQueue;
    private ImageLoader imageLoader;
    //Don't want other classes to use the constructor hence, it is private #singleton
    private VolleySingleton(){
        //requires an application context. Hence, an application java class should be created
        requestQueue = Volley.newRequestQueue(AppApplication.getAppContext());
        //image cache to store the image for easy reuse
        imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache() {

            /*
            The object that will work as the cache. Hence, we use LRU cache.
            A cache that holds strong references to a limited number of values.
            Each time a value is accessed, it is moved to the head of a queue.
            When a value is added to a full cache, the value at the end of that queue is evicted
            and may become eligible for garbage collection.
             */
            //standard method to get size, division by 8*1024 is due to conversion from bits & bytes
            private LruCache<String, Bitmap> icache = new LruCache<>((int) (Runtime.getRuntime().maxMemory()/(8*1024)));

            @Override
            public Bitmap getBitmap(String url) {
                return icache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                icache.put(url, bitmap);
            }
        });
    }

    //other classes can implement an instance of VolleySingleton
    public static VolleySingleton getInstance (){
        if(sInstance == null){
            sInstance = new VolleySingleton();
        }
        return sInstance;
    }

    public RequestQueue getRequestQueue(){
        return requestQueue;
    }

    public ImageLoader getImageLoader(){
        return imageLoader;
    }
}
