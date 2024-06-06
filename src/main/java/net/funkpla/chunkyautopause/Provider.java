package net.funkpla.chunkyautopause;

public final class Provider {
    private static ChunkyAutoPause instance;
    private Provider() {throw new UnsupportedOperationException("This class cannot be instantiated.");}

    public static ChunkyAutoPause get() {
        if (instance == null) {
            throw new IllegalStateException("ChunkyAutoPause is not loaded");
        }
        return instance;
    }

    static void register(ChunkyAutoPause instance){
        Provider.instance = instance;
    }
    static void unregister(){
        Provider.instance = null;
    }
}
