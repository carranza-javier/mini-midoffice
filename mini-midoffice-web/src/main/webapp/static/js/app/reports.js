/* =========================================================
   reports.js — Reports section: on-screen tables + file downloads
   ========================================================= */
var Reports = (function ($) {
    'use strict';

    var eventsWired = false;
    var tplRows = null;

    function getTplRows() {
        if (!tplRows) {
            tplRows = Handlebars.compile($('#tpl-report-rows').html());
        }
        return tplRows;
    }

    function init() {
        if (!eventsWired) {
            $('#report-apply-dates').on('click', function () {
                loadAll($('#report-from').val(), $('#report-to').val());
            });
            $('#report-clear-dates').on('click', function () {
                $('#report-from').val('');
                $('#report-to').val('');
                loadAll('', '');
            });
            eventsWired = true;
        }
        loadAll($('#report-from').val(), $('#report-to').val());
    }

    function loadAll(from, to) {
        var entries = [
            { endpoint: 'by-destination', tbody: '#report-dest-tbody'  },
            { endpoint: 'by-provider',    tbody: '#report-prov-tbody'   },
            { endpoint: 'by-month',       tbody: '#report-month-tbody'  }
        ];

        entries.forEach(function (e) {
            var $tbody = $(e.tbody);
            $tbody.html(
                '<tr><td colspan="4" class="text-muted text-center">' +
                '<span class="glyphicon glyphicon-refresh spinning"></span> Loading…' +
                '</td></tr>'
            );
            API.reports.fetch(e.endpoint, from, to)
                .done(function (data) {
                    if (!data.rows || data.rows.length === 0) {
                        $tbody.html('<tr><td colspan="4" class="text-muted text-center">No data.</td></tr>');
                    } else {
                        $tbody.html(getTplRows()({ rows: data.rows }));
                    }
                })
                .fail(function () {
                    $tbody.html('<tr><td colspan="4" class="text-danger text-center">Could not load report.</td></tr>');
                });
        });
    }

    /**
     * Builds the download URL with the currently applied date range and navigates
     * to it — the browser triggers the file download.
     *
     * @param {string} endpoint  'by-destination' | 'by-provider' | 'by-month'
     * @param {string} format    'csv' | 'excel'
     */
    function download(endpoint, format) {
        var from = $('#report-from').val();
        var to   = $('#report-to').val();
        var url  = '/api/reports/' + endpoint + '?format=' + format;
        if (from) url += '&from=' + from;
        if (to)   url += '&to='   + to;
        window.location.href = url;
    }

    return { init: init, download: download };

}(jQuery));
