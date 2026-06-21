/* =========================================================
   dashboard.js — stats panel + recent profiles widget
   ========================================================= */
var Dashboard = (function ($) {
    'use strict';

    var initialized = false;

    function init() {
        if (!initialized) {
            bindEvents();
            initialized = true;
        }
        load();
    }

    function load() {
        $('#stat-profiles').html('<span class="glyphicon glyphicon-refresh spinning"></span>');
        $('#dashboard-recent-profiles').html(
            '<tr><td colspan="5" class="text-muted text-center">' +
            '<span class="glyphicon glyphicon-refresh spinning"></span> Loading…</td></tr>'
        );

        API.profiles.list().done(function (profiles) {
            $('#stat-profiles').text(profiles.length);

            if (!profiles.length) {
                $('#dashboard-recent-profiles').html(
                    '<tr><td colspan="5" class="text-muted text-center">No profiles yet. ' +
                    '<a href="#" data-section="profiles">Create one →</a></td></tr>'
                );
                return;
            }

            var rows = '';
            profiles.slice(0, 10).forEach(function (p) {
                var name = Handlebars.escapeExpression(p.fullName || (p.firstName + ' ' + p.lastName));
                var email = Handlebars.escapeExpression(p.email || '');
                var company = Handlebars.escapeExpression(p.company || '—');
                rows += '<tr>' +
                    '<td>' + p.id + '</td>' +
                    '<td><strong>' + name + '</strong></td>' +
                    '<td>' + email + '</td>' +
                    '<td>' + company + '</td>' +
                    '<td>' +
                    '<button class="btn btn-xs btn-info btn-dash-bookings" data-id="' + p.id + '">' +
                    '<span class="glyphicon glyphicon-list"></span> Bookings' +
                    '</button>' +
                    '</td>' +
                    '</tr>';
            });
            $('#dashboard-recent-profiles').html(rows);
        });
    }

    function bindEvents() {
        $(document).on('click', '.btn-dash-bookings', function () {
            var tid = $(this).data('id');
            Bookings.loadForTraveller(tid);
            $('[data-section="bookings"]').trigger('click');
        });
    }

    return {init: init};

}(jQuery));
