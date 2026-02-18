package com.mongraphe.graphui.interfaces;

import com.mongraphe.graphui.app.CommandBus;

public interface CommandBusLinkedI<C> {

    void setBus(CommandBus<C> bus);
}