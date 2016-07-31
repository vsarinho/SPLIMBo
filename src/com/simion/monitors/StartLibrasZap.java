package com.simion.monitors;

public class StartLibrasZap {

	public static void main(String[] args) {
				
		String LibrasZap_ACCESS_TOKEN = "EAAWmGJZBjLQYBAD3wquCKsKA3msVuMfJsAiHZBwWZCaDk1bFnc61YSojFhMGrX4pHhT5VIRNoPZBl20BAH10fLo4n7o53ZC5wJa2dOmjcFqtbLjfJZBP9I40vPTr98Mn96dVlu6AjRtMlycXDHXdueSnkLQyP2HtvUbuRAUpeuwZDZD";
		FacebookMonitor m = new FacebookMonitor(LibrasZap_ACCESS_TOKEN);
	    m.start();
	}

}
