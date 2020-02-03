package com.cyworld.social.data.entity.ranks;

import com.cyworld.social.data.entity.base.i_deep_clone;
import lombok.Builder;
import lombok.Data;

import java.util.TreeSet;
import java.util.stream.Collectors;

@Data
@Builder
public class ranks implements i_deep_clone<ranks> {
    String id;
    @Builder.Default
    TreeSet<ranks_data> datas=new TreeSet<>();

    @Override
    public ranks deep_clone() {
        return ranks.deep_clone(this);
    }


    public static ranks deep_clone(ranks data){
        ranks new_data=ranks
                .builder()
                .id(data.id)
                .build();
        new_data.datas=data.datas.stream().map(o->{return o.deep_clone();}).collect(Collectors.toCollection(TreeSet::new));
        return new_data;
    }
}
