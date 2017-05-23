package org.reviewboard.rbgerrit;

import com.google.inject.AbstractModule;


/**
 * The rbgerrit module.
 *
 * This module is responsible for installing our extensions to Gerrit's REST API.
 */
public class Module extends AbstractModule {
    /**
     * Install our extensions to Gerrit's REST API.
     */
    @Override
    protected void configure() {
    }
}
