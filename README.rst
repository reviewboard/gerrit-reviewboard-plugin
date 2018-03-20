=========================
gerrit-reviewboard-plugin
=========================

A plugin for Gerrit_ for integrating with `Review Board`_. This plugin
extends Gerrit's API to make it suitable for a Review Board hosting service.

.. _Gerrit: https://www.gerritcodereview.com/
.. _Review Board: https://www.reviewboard.org/


Installation
============

We provide builds for this plugin on our `official releases site`_. Download
the ``.jar`` file and place it into your ``gerrit/plugins/`` directory and
restart Gerrit. Follow the instructions_ in the official Review Board
documentation for information on adding Gerrit-hosted repositories to Review
Board.

.. _official releases site:
   https://downloads.reviewboard.org/releases/gerrit-reviewboard-plugin/

.. _instructions:
   https://www.reviewboard.org/docs/manual/latest/admin/configuration/repositories/gerrit/


Developing
==========

This project is built with maven. To create the plugin ``jar``, run::

    mvn package


The resulting plugin will be in ``target/``. Copy this into
``/path/to/gerrit/plugins/`` and restart Gerrit.
