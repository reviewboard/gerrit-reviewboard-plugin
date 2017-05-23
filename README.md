gerrit-reviewboard-plugin
=========================

A plugin for [Gerrit][gerrit] for integrating with [Review Board][reviewboard].
This plugin extends Gerrit's API to make it suitable for a Review Board hosting
service.

[gerrit]: https://www.gerritcodereview.com/
[reviewboard]: https://reviewboard.org/


## Building

This project is built with maven. To create the plugin `jar`, run:

    mvn package

The resulting plugin will be in `target/`. Copy this into
`/path/to/gerrit/plugins/` and restart Gerrit.
