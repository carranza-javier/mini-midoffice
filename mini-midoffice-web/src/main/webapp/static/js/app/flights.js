/* =========================================================
   flights.js — Sabre flight search form + results + reserve
   ========================================================= */
var Flights = (function ($) {
    'use strict';

    var tpl;
    var lastTravellerId = null;
    var lastCurrency = 'USD';
    var eventsWired = false;

    function init() {
        tpl = Handlebars.compile($('#tpl-flights').html());
        if (!eventsWired) {
            setDefaultDates();
            bindEvents();
            eventsWired = true;
        }
    }

    function setDefaultDates() {
        var from = new Date();
        from.setDate(from.getDate() + 14);
        var to = new Date(from);
        to.setDate(to.getDate() + 14);
        $('#flight-from-date').val(isoDate(from));
        $('#flight-to-date').val(isoDate(to));
    }

    function isoDate(d) {
        return d.toISOString().slice(0, 10);
    }

    function bindEvents() {
        $('#flight-search-form').on('submit', function (e) {
            e.preventDefault();
            search();
        });

        $(document).on('click', '.btn-reserve-flight', function () {
            var $btn = $(this);
            reserve(
                $btn.data('flightkey'),
                parseFloat($btn.data('price')),
                $btn.data('currency')
            );
        });
    }

    function search() {
        var origin = $('#flight-origin').val().trim().toUpperCase();
        if (!origin) {
            window.showAlert('Origin is required (e.g. <strong>JFK</strong>).', 'warning');
            return;
        }

        var params = {
            origin:         origin,
            fromDate:       $('#flight-from-date').val(),
            toDate:         $('#flight-to-date').val(),
            passengerCount: parseInt($('#flight-passengers').val()) || 1,
            currencyCode:   $('#flight-currency').val().trim().toUpperCase() || 'USD'
        };

        var dest = $('#flight-destination').val().trim().toUpperCase();
        if (dest) params.destination = dest;

        var tid = parseInt($('#flight-traveller-id').val());
        if (tid) {
            lastTravellerId = tid;
        } else {
            lastTravellerId = null;
        }
        lastCurrency = params.currencyCode;

        $('#flights-results').html(
            '<p class="text-muted">' +
            '<span class="glyphicon glyphicon-refresh spinning"></span> Searching Sabre…</p>'
        );

        API.flights.search(params).done(function (options) {
            if (!options || options.length === 0) {
                $('#flights-results').html(
                    '<div class="alert alert-warning">' +
                    '<span class="glyphicon glyphicon-warning-sign"></span> ' +
                    'No results returned by Sabre for origin <strong>' + origin + '</strong>. ' +
                    'Try <strong>JFK</strong> — the cert environment only has flight data for US origins.' +
                    '</div>'
                );
                return;
            }

            /* Annotate each option with canReserve flag */
            $.each(options, function (i, o) {
                o.canReserve = !!(lastTravellerId && o.flightKey);
            });

            $('#flights-results').html(tpl({options: options}));
        });
    }

    function reserve(flightKey, price, currency) {
        if (!lastTravellerId) {
            window.showAlert('Enter a <strong>Traveller ID</strong> in the search form to reserve.', 'warning');
            return;
        }
        if (!flightKey) {
            window.showAlert('No flight key available for this offer (cert limitation for non-US origins).', 'warning');
            return;
        }

        var data = {
            flightKey:     flightKey,
            travellerId:   lastTravellerId,
            searchedPrice: price,
            currencyCode:  currency || lastCurrency
        };

        API.bookings.create(data).done(function (booking) {
            window.showAlert(
                'Flight reserved! Booking <strong>#' + booking.id + '</strong> — ' +
                'confirmed price: <strong>' + booking.confirmedPrice + ' ' + (currency || lastCurrency) + '</strong>',
                'success'
            );
            Bookings.loadForTraveller(lastTravellerId);
            $('[data-section="bookings"]').trigger('click');
        });
    }

    return {init: init};

}(jQuery));
