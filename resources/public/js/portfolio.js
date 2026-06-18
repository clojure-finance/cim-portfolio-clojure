document.addEventListener('DOMContentLoaded', function() {
  // Select all divs with a data-plot attribute
  const chartDivs = document.querySelectorAll('[data-plot]');
  
  chartDivs.forEach(div => {
    const plotData = JSON.parse(div.dataset.plot);
    Plotly.newPlot(div.id, [plotData]);
  });

  // Logic for portfolio volatility graphs

  // Button to change volatility graphs
  const volatilityChartDiv = document.getElementById("rolling-ewma-volatility");
  const volatilityBtn = document.getElementById("lambdaSwitch");

  volatilityBtn.addEventListener("click", function() {

    // Toggle between λ=0.94 and λ=0.97
    const currentTitle = volatilityChartDiv.parentElement.parentElement.querySelector('h2'); 

    if (volatilityBtn.textContent.includes("0.97")) {
      Plotly.newPlot(volatilityChartDiv.id, [JSON.parse(volatilityChartDiv.dataset.altPlot)]);
      volatilityBtn.textContent = "Switch to λ = 0.94";
      currentTitle.textContent = "30-Day Annualized EWMA Rolling Volatility of Portfolio (λ = 0.97)";
      } 
    else { 
      Plotly.newPlot(volatilityChartDiv.id, [JSON.parse(volatilityChartDiv.dataset.plot)]);
      volatilityBtn.textContent = "Switch to λ = 0.97";
      currentTitle.textContent = "30-Day Annualized EWMA Rolling Volatility of Portfolio (λ = 0.94)";
      }});

  // Logic for portfolio sharpe ratio graphs

  // Button to change sharpe ratio graphs
  const sharpeChartDiv = document.getElementById("rolling-sharpe-ratio");
  const sharpeBtn = document.getElementById("sharpeLambdaSwitch");

  sharpeBtn.addEventListener("click", function() {

    // Toggle between λ=0.94 and λ=0.97
    const currentTitle = sharpeChartDiv.parentElement.parentElement.querySelector('h2'); 

    if (sharpeBtn.textContent.includes("0.97")) {
      Plotly.newPlot(sharpeChartDiv.id, [JSON.parse(sharpeChartDiv.dataset.altPlot)]);
      sharpeBtn.textContent = "Switch to λ = 0.94";
      currentTitle.textContent = "30-Day Annualized Rolling Sharpe Ratio (EWMA λ = 0.97)";
      } 
    else { 
      Plotly.newPlot(sharpeChartDiv.id, [JSON.parse(sharpeChartDiv.dataset.plot)]);
      sharpeBtn.textContent = "Switch to λ = 0.97";
      currentTitle.textContent = "30-Day Annualized Rolling Sharpe Ratio (EWMA λ = 0.94)";
      }});
});