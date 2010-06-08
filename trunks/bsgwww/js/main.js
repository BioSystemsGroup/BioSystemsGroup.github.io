
function sizeWindow() {
	var isIE = (navigator.appName.indexOf("Microsoft") != -1);
	var isWin = (navigator.appVersion.indexOf("Windows") != -1);
	var isMac = (navigator.appVersion.indexOf('Mac') != -1);
	var mainTable = document.getElementById("main_table");
	var leftBox = document.getElementById("left_box");	
	var rightBox = document.getElementById("right_box");
	leftBox.style.height = mainTable.scrollHeight;
	rightBox.style.height = mainTable.scrollHeight;
	if (isIE && isMac) {
		var leftCorner = document.getElementById("left_corner");
		leftCorner.style.left = "153px";
	}
}

window.onload = sizeWindow;
