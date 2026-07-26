package com.ensemblu.axiom.core.navigation;

import com.ensemblu.axiom.api.TargetNavigator;
import com.ensemblu.axiom.core.validation.Result;
import com.ensemblu.axiom.core.foundation.DataCast;
import com.ensemblu.axiom.core.foundation.Dop;

import java.util.Objects;


public interface SourceBehavior {
    Source follow(Object key);
    Source inIndex(int idx);
    boolean exists();
    Object getValue();

    default TargetNavigator navigate() {
        return new TargetNavigator() {
            @Override
            public <T> Result<T> execute(DataCast.Protocol protocol) {
                return Objects.isNull(getValue()) //
                        ? Result.failure("Source path does not exist")//
                        : DataCast.cast(Dop.resolve(getValue()), protocol);
            }
        };
    }
}