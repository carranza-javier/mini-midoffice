/* =========================================================
   bookings.js — list by travellerId, detail panel, cancel
   ========================================================= */
var Bookings = (function ($) {
    'use strict';

    var tplList;
    var tplDetail;
    var currentTravellerId = null;
    var eventsWired = false;

    function init() {
        tplList   = Handlebars.compile($('#tpl-bookings-list').html());
        tplDetail = Handlebars.compile($('#tpl-booking-detail').html());
        if (!eventsWired) {
            bindEvents();
            eventsWired = true;
        }
        if (currentTravellerId) {
            loadBookings(currentTravellerId);
        }
    }

    /* Called externally (from flights.js and dashboard.js) */
    function loadForTraveller(travellerId) {
        currentTravellerId = travellerId;
        $('#bookings-traveller-id').val(travellerId);
        loadBookings(travellerId);
    }

    function loadBookings(travellerId) {
        $('#bookings-tbody').html(
            '<tr><td colspan="7" class="text-muted text-center">' +
            '<span class="glyphicon glyphicon-refresh spinning"></span> Loading…</td></tr>'
        );
        $('#booking-detail').hide().empty();

        API.bookings.list(travellerId).done(function (bookings) {
            $.each(bookings, function (i, b) {
                b.canCancel = (b.status !== 'CANCELLED');
            });
            $('#bookings-tbody').html(tplList({bookings: bookings}));
        });
    }

    function bindEvents() {
        /* Manual load */
        $('#bookings-load-btn').on('click', function () {
            var tid = parseInt($('#bookings-traveller-id').val());
            if (!tid) {
                window.showAlert('Enter a valid Traveller ID.', 'warning');
                return;
            }
            currentTravellerId = tid;
            loadBookings(tid);
        });
        $('#bookings-traveller-id').on('keypress', function (e) {
            if (e.which === 13) $('#bookings-load-btn').trigger('click');
        });

        /* Detail — delegated */
        $(document).on('click', '.btn-booking-detail', function () {
            var id = $(this).data('id');
            API.bookings.get(id).done(function (b) {
                b.canCancel = (b.status !== 'CANCELLED');
                $('#booking-detail')
                    .html(tplDetail({booking: b}))
                    .show();
                $('html, body').animate(
                    {scrollTop: $('#booking-detail').offset().top - 80},
                    300
                );
            });
        });

        /* Cancel — delegated (works both in list and in detail panel) */
        $(document).on('click', '.btn-cancel-booking', function () {
            var id = $(this).data('id');
            if (!confirm('Cancel booking #' + id + '? This cannot be undone.')) return;
            API.bookings.cancel(id).done(function () {
                window.showAlert('Booking <strong>#' + id + '</strong> cancelled.', 'success');
                if (currentTravellerId) loadBookings(currentTravellerId);
            });
        });
    }

    return {
        init:             init,
        loadForTraveller: loadForTraveller
    };

}(jQuery));
