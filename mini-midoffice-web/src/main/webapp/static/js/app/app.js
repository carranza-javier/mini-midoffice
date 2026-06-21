/* =========================================================
   app.js — bootstrap, Handlebars helpers, section router
   Must load last (after all modules are defined).
   ========================================================= */
$(function () {
    'use strict';

    /* ----- Handlebars helpers ----------------------------------------- */

    Handlebars.registerHelper('statusBadge', function (status) {
        var map = {RESERVED: 'success', CANCELLED: 'default'};
        var cls = map[status] || 'default';
        return new Handlebars.SafeString(
            '<span class="label label-' + cls + '">' + (status || '—') + '</span>'
        );
    });

    Handlebars.registerHelper('statusPanel', function (status) {
        var map = {RESERVED: 'success', CANCELLED: 'default'};
        return map[status] || 'default';
    });

    Handlebars.registerHelper('durationMins', function (mins) {
        if (!mins) return '—';
        var h = Math.floor(mins / 60);
        var m = mins % 60;
        return (h > 0 ? h + 'h ' : '') + m + 'm';
    });

    Handlebars.registerHelper('orDash', function (val) {
        return (val !== null && val !== undefined && val !== '') ? val : '—';
    });

    /* ----- Global alert ------------------------------------------------ */

    window.showAlert = function (msg, type) {
        type = type || 'danger';
        var $a = $([
            '<div class="alert alert-' + type + ' alert-dismissible" role="alert">',
            '<button type="button" class="close" data-dismiss="alert"><span>&times;</span></button>',
            msg,
            '</div>'
        ].join(''));
        $('#alert-container').empty().append($a);
        if (type !== 'danger') {
            setTimeout(function () { $a.fadeOut(400, function () { $(this).remove(); }); }, 4000);
        }
    };

    /* ----- Section router ---------------------------------------------- */

    var modules = {
        dashboard: Dashboard,
        profiles:  Profiles,
        flights:   Flights,
        bookings:  Bookings,
        reports:   Reports
    };

    function showSection(name) {
        $('.mu-section').hide();
        $('#section-' + name).show();
        $('.nav li').removeClass('active');
        $('.nav [data-section="' + name + '"]').closest('li').addClass('active');
        if (modules[name]) {
            modules[name].init();
        }
    }

    $(document).on('click', '[data-section]', function (e) {
        e.preventDefault();
        showSection($(this).data('section'));
    });

    /* ----- Start on Dashboard ------------------------------------------ */
    showSection('dashboard');
});
