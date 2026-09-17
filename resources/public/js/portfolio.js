document.addEventListener('DOMContentLoaded', function() {
  // Select all divs with a data-plot attribute
  const chartDivs = document.querySelectorAll('[data-plot]');
  
  // The portfolio-based time series (.miscChart) all share the same trading dates, so pin their
  // x-axis to the full date span: autorange skips null points, which would make a late-starting
  // series (the Sharpe ratio is masked for its first month) begin its axis a month after the others.
  const layoutFor = (div, plotData) => {
    const layout = div.classList.contains('logChart') ? { yaxis: { type: 'log' } } : {};
    if (div.classList.contains('miscChart') && plotData.x && plotData.x.length > 1) {
      layout.xaxis = { range: [plotData.x[0], plotData.x[plotData.x.length - 1]] };
    }
    return layout;
  };

  chartDivs.forEach(div => {
    const plotData = JSON.parse(div.dataset.plot);
    if (plotData == null) return; // A missing dataset must not abort rendering of the remaining charts
    Plotly.newPlot(div.id, [plotData], layoutFor(div, plotData));
  });

  // Logic for portfolio volatility graphs

  // Button to change volatility graphs
  const volatilityChartDiv = document.getElementById("rolling-ewma-volatility");
  const volatilityBtn = document.getElementById("lambdaSwitch");

  volatilityBtn.addEventListener("click", function() {

    // Toggle between λ=0.94 and λ=0.97
    const currentTitle = volatilityChartDiv.parentElement.parentElement.querySelector('h2'); 

    if (volatilityBtn.textContent.includes("0.97")) {
      const altData = JSON.parse(volatilityChartDiv.dataset.altPlot);
      Plotly.newPlot(volatilityChartDiv.id, [altData], layoutFor(volatilityChartDiv, altData));
      volatilityBtn.textContent = "Switch to λ = 0.94";
      currentTitle.textContent = "Annualized EWMA Volatility of Portfolio (λ = 0.97)";
      }
    else {
      const defaultData = JSON.parse(volatilityChartDiv.dataset.plot);
      Plotly.newPlot(volatilityChartDiv.id, [defaultData], layoutFor(volatilityChartDiv, defaultData));
      volatilityBtn.textContent = "Switch to λ = 0.97";
      currentTitle.textContent = "Annualized EWMA Volatility of Portfolio (λ = 0.94)";
      }});

  // Logic for portfolio sharpe ratio graphs

  // Button to change sharpe ratio graphs
  const sharpeChartDiv = document.getElementById("rolling-sharpe-ratio");
  const sharpeBtn = document.getElementById("sharpeLambdaSwitch");

  sharpeBtn.addEventListener("click", function() {

    // Toggle between the 1-year-window default and λ=0.94
    const currentTitle = sharpeChartDiv.parentElement.parentElement.querySelector('h2');

    if (sharpeBtn.textContent.includes("0.94")) {
      const altData = JSON.parse(sharpeChartDiv.dataset.altPlot);
      Plotly.newPlot(sharpeChartDiv.id, [altData], layoutFor(sharpeChartDiv, altData));
      sharpeBtn.textContent = "Switch to 1-year window";
      currentTitle.textContent = "Annualized EWMA Sharpe Ratio (λ = 0.94)";
      }
    else {
      const defaultData = JSON.parse(sharpeChartDiv.dataset.plot);
      Plotly.newPlot(sharpeChartDiv.id, [defaultData], layoutFor(sharpeChartDiv, defaultData));
      sharpeBtn.textContent = "Switch to λ = 0.94";
      currentTitle.textContent = "Annualized EWMA Sharpe Ratio (1-year window)";
      }});
});