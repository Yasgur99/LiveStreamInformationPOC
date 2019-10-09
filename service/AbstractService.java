package com.mergg.common.persistence.service;

import com.mergg.common.persistence.model.IEntity;

public abstract class AbstractService<T extends IEntity> extends AbstractRawService<T> implements IService<T> {

    public AbstractService() {
        super();
    }

    // API

    // find - one

}
