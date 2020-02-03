package com.cyworld.social.data.utils;

import com.cyworld.social.data.entity.base.i_deep_clone;
import lombok.Data;

@Data
public class reset_by_timer_point {
    public static interface target extends i_deep_clone<target>{
        void do_reset();
    }

    public static class controller<T extends target>
            implements i_deep_clone<controller> {
        public T target;
        public long waiting_timer=0;
        public void try_reset(long reset_timer){
            if(waiting_timer>=System.currentTimeMillis())
                return;
            target.do_reset();
            waiting_timer=reset_timer;
        }

        public controller(){

        }
        public controller(T target){
            this.target=target;
            waiting_timer=0;
        }
        public controller(T target,long waiting_timer){
            this.target=target;
            waiting_timer=waiting_timer;
        }

        @Override
        public controller<T> deep_clone() {
            controller res=new controller();
            res.waiting_timer=this.waiting_timer;
            res.target=this.target.deep_clone();
            return res;
        }
    }
}
