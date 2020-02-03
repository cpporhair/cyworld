package com.cyworld.social.data.entity.ranks;

import com.cyworld.social.data.service.ranks.srv_add_value;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
@Builder
public class ranks_data implements Comparable<ranks_data>{
    private static final Logger logger = LoggerFactory.getLogger(srv_add_value.class.getName());
    String ID;
    Long compare_value;
    Long time;
    String inner_data;

    @Override
    public int compareTo(ranks_data o) {
        if (this.compare_value.equals(o.compare_value)){
            if(this.time.equals(o.time)) {
                return this.ID.compareTo(o.ID);
            }
            else {
                return this.time.compareTo(o.time);
            }
        }
        else{
            return o.compare_value.compareTo(this.compare_value);
        }
    }

    public ranks_data deep_clone() {
        return deep_clone(this);
    }
    public static ranks_data deep_clone(ranks_data data){
        return ranks_data
                .builder()
                .ID(data.ID)
                .time(data.time)
                .compare_value(data.compare_value)
                .inner_data(data.inner_data)
                .build();
    }
}
