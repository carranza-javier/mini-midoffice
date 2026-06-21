/* =========================================================
   api.js — centralised AJAX wrapper around /api/*
   All methods return jQuery Deferred objects.
   Errors are shown via window.showAlert (defined in app.js).
   ========================================================= */
var API = (function ($) {
    'use strict';

    var BASE = '/api';

    function ajax(opts) {
        return $.ajax(opts).fail(function (xhr) {
            var msg = 'Unexpected error';
            try {
                var body = JSON.parse(xhr.responseText);
                msg = body.message || body.error || msg;
            } catch (e) { /* ignore parse error */ }
            if (typeof window.showAlert === 'function') {
                window.showAlert('<strong>Error ' + (xhr.status || '') + ':</strong> ' + msg, 'danger');
            }
        });
    }

    return {

        profiles: {
            list: function (q, offset, limit) {
                var data = {offset: offset || 0, limit: limit || 50};
                if (q) data.q = q;
                return ajax({url: BASE + '/profiles', data: data});
            },
            get: function (id) {
                return ajax({url: BASE + '/profiles/' + id});
            },
            create: function (data) {
                return ajax({
                    url: BASE + '/profiles',
                    type: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify(data)
                });
            },
            update: function (id, data) {
                return ajax({
                    url: BASE + '/profiles/' + id,
                    type: 'PUT',
                    contentType: 'application/json',
                    data: JSON.stringify(data)
                });
            }
        },

        flights: {
            search: function (params) {
                return ajax({url: BASE + '/flights/search', data: params});
            }
        },

        bookings: {
            list: function (travellerId) {
                return ajax({url: BASE + '/bookings', data: {travellerId: travellerId}});
            },
            get: function (id) {
                return ajax({url: BASE + '/bookings/' + id});
            },
            create: function (data) {
                return ajax({
                    url: BASE + '/bookings',
                    type: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify(data)
                });
            },
            cancel: function (id) {
                return ajax({url: BASE + '/bookings/' + id + '/cancel', type: 'PUT'});
            }
        },

        reports: {
            fetch: function (endpoint, from, to) {
                var data = {};
                if (from) data.from = from;
                if (to)   data.to   = to;
                return ajax({url: BASE + '/reports/' + endpoint, data: data});
            }
        }

    };
}(jQuery));
