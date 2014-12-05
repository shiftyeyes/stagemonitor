(function () {
	var table;
	var selectedName;

	plugins.push(
		{
			id: "request-metrics",
			label: "Requests",
			htmlPath: "tabs/metrics/request-metrics.html",
			graphs: [
				{
					bindto: '#requests',
					min: 0,
					format: 'ms',
					fill: 0.1,
					columns: [
						["timers", /request.(All).server.time.total/, "mean"]
					]
				},
				{
					bindto: '#requests',
					disabled: true,
					min: 0,
					format: 'ms',
					fill: 0.1,
					columns: [
						["timers", /request.(GET-\|).server.time.total/, "mean"]
					]
				}
			],
			timerTable: {
				bindto: "#request-table",
				timerRegex: /request.([^\.]+).server.time.total/
			},
			onHtmlInitialized: function () {
				initRequestTable();
			},
			onMetricsReceived: function (metrics) {
				table.fnClearTable();
				table.fnAddData(getData(metrics));
				restoreSelectedRow();
			}
		}
	);

	function restoreSelectedRow() {
		$(table.fnSettings().aoData).each(function () {
			var requestName = table.fnGetData(this.nTr).name;
			if (requestName == selectedName) {
				$(this.nTr).addClass('selected');
			}
		});
	}

	function onRowSelected() {

	}

	function onRowDeselected() {
		graphRenderer.disableGraph("#requests");
	}

	function initRequestTable() {
		table = $('#request-table').dataTable({
			"bJQueryUI": true,
			"sPaginationType": "full_numbers",
			"bLengthChange": false,
			"aoColumns": [
				{
					"sTitle": "Name",
					"mData": "name"
				},
				{
					"sTitle": "Requests/s",
					"mData": "m1_rate"
				},
				{
					"sTitle": "Max",
					"mData": "max"
				},
				{
					"sTitle": "Mean",
					"mData": "mean"
				},
				{
					"sTitle": "Min",
					"mData": "min"
				},
				{
					"sTitle": "p50",
					"mData": "p50"
				},
				{
					"sTitle": "p95",
					"mData": "p95"
				},
				{
					"sTitle": "Std. Dev.",
					"mData": "stddev"
				}
			]
		});
		table.find('tbody').on('click', 'tr', function () {
			if (!$(this).hasClass('selected')) {
				// select
				table.$('tr.selected').removeClass('selected');
				$(this).addClass('selected');
				selectedName = table.fnGetData(table.fnGetPosition(this)).name;
				onRowSelected();
			} else {
				// deselect
				selectedName = null;
				$(this).removeClass('selected');
				onRowDeselected();
			}
		});
	}

	function getData(metrics) {
		var data = [];
		for (timerName in metrics.timers) {
			var match = new RegExp(/request.([^\.]+).server.time.total/).exec(timerName);
			if (match != null) {
				var timer = metrics.timers[timerName];
				timer.name = match[1];

				// round all numbers
				$.each(timer, function (timerMetric, timerMetricValue) {
					if (typeof timerMetricValue == 'number') {
						timer[timerMetric] = timerMetricValue.toFixed(2)
					}
				});
				data.push(timer)
			}
		}
		return data;
	}
}());

