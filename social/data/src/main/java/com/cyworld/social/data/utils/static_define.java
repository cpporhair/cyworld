package com.cyworld.social.data.utils;

public class static_define {
    public final static String no_in_transaction = "NONE";
    public final static long data_actor_expire_second = 10;//1800;
    public final static long data_actor_save_signal_delay = 5;

    public final static boolean check_same_transaction(String source, String checker) {
        return source.equals(no_in_transaction) || source.equals(checker);
    }

    public static final class throwable_empty_actor extends Throwable{
        public throwable_empty_actor(String s){
            super(s);
        }
    }
}
