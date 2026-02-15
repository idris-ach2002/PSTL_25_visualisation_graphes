package com.mongraphe.graphui.interfaces;

import com.mongraphe.graphui.app.ApplicationContext;

public interface ContextAware {
    void setContext(ApplicationContext context);
}