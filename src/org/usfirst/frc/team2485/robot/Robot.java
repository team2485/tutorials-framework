package org.usfirst.frc.team2485.robot;

import org.usfirst.frc.com.kauailabs.nav6.frc.IMUAdvanced;
import org.usfirst.frc.team2485.auto.Sequencer;
import org.usfirst.frc.team2485.auto.SequencerFactory;
import org.usfirst.frc.team2485.subsystems.Clapper;
import org.usfirst.frc.team2485.subsystems.Claw;
import org.usfirst.frc.team2485.subsystems.ContainerCommandeerer;
import org.usfirst.frc.team2485.subsystems.DriveTrain;
import org.usfirst.frc.team2485.subsystems.RatchetSystem;
import org.usfirst.frc.team2485.subsystems.Rollers;
import org.usfirst.frc.team2485.subsystems.Strongback;
import org.usfirst.frc.team2485.util.CombinedSpeedController;
import org.usfirst.frc.team2485.util.Controllers;
import org.usfirst.frc.team2485.util.DualEncoder;
import org.usfirst.frc.team2485.util.ToteCounter;

import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.AnalogPotentiometer;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Relay;
import edu.wpi.first.wpilibj.SerialPort;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.Talon;
import edu.wpi.first.wpilibj.Ultrasonic;
import edu.wpi.first.wpilibj.Ultrasonic.Unit;
import edu.wpi.first.wpilibj.VictorSP;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * @author Anoushka Bose
 * @author Aidan Fay
 * @author Ben Clark
 * @author Patrick Wamsley
 * @author Camille Considine
 * @author Mike Maunu
 */

//0 ON PCM IS THE CLAW
public class Robot extends IterativeRobot {

	// Subsystems
	public static DriveTrain drive;
	public static Strongback strongback;
	public static Clapper clapper;
	public static Rollers rollers;
	public static RatchetSystem ratchet;
	public static Claw claw;
	public static ContainerCommandeerer containerCommandeerer;
	public static ToteCounter toteCounter;
	// private CameraServer camServer;

	// Speed Controllers
	private CombinedSpeedController leftDrive, rightDrive, centerDrive;
	private CombinedSpeedController clapperLifter;
	private SpeedController strongbackMotor, clawMotor;
	private SpeedController leftRoller, rightRoller;

	// Solenoids
	private Solenoid centerWheelSuspension;
	private Solenoid ratchetLatchActuator;
	private Solenoid shortFingerActuators, longFingerActuators;
	private Solenoid commandeererSolenoidRight, commandeererSolenoidLeft;
	private Solenoid clapperActuator, clawSolenoid;

	// Sensors
	private Encoder leftEnc, rightEnc, centerEnc;
	private DualEncoder dualEncoderVelCalc;

	private IMUAdvanced imu;

	private DigitalInput clapperSafetyLimitSwitch;
	private DigitalInput toteDetectorLimitSwitch;
	private DigitalInput pressureSwitch;
	
	private Ultrasonic sonic;
	private final int sonicPingChannel = 4, sonicEchoChannel = 5; 
	
	private AnalogPotentiometer clapperPot;
	private AnalogPotentiometer clawPot;

	// Tote Count
	private long timeLastToteCountProcessed;
	private long BUTTON_MULTIPLE_CLICK_MIN_DELAY = 250;
	
	// Coopertition
	private long timeLastDecreaseCoopStackProcessed;
	private long timeLastIncreaseCoopStackProcessed;
	private int numberOfTotesOnStep = 0;

	// Sequences && Auto
	private Sequencer autoSequence, currTeleopSequence, adjustContainerSequence;
	private boolean finishedRotating = false;
	private int degrees;
	
	private static double AUTO_WALL_SONIC_OFFSET; 

	// Data
	private double curPos, lastPos;
	private static double currVelocity;
	private double lastVelocity;
	
	private double buttonMashCount = 1; 

	// Other needed variables
	private Relay compressorSpike;

	public void robotInit() {

		 leftDrive = new CombinedSpeedController(new Talon(14), new VictorSP(15));
		 rightDrive = new CombinedSpeedController(new Talon(0), new VictorSP(2));
		 centerDrive = new CombinedSpeedController(new Talon(11), new VictorSP(12)); 
		 clapperLifter = new CombinedSpeedController(new VictorSP(6), new VictorSP(8));
		 strongbackMotor = new Talon(3);
		 leftRoller = new Talon(7);
		 rightRoller = new Talon(9);
		 clawMotor = new VictorSP(13);

		 centerWheelSuspension = new Solenoid(3);
		 ratchetLatchActuator = new Solenoid(1);
		 commandeererSolenoidLeft = new Solenoid(4);
		 commandeererSolenoidRight = new Solenoid(5);
		 clapperActuator	= new Solenoid(2);
		 clawSolenoid 		= new Solenoid(0);
 
		 leftEnc 	= new Encoder(0, 1);
		 rightEnc 	= new Encoder(2, 3);
		 centerEnc 	= new Encoder(21, 22); 
		 
		 sonic 		= new Ultrasonic(sonicPingChannel, sonicEchoChannel, Unit.kInches); 
		 sonic.setAutomaticMode(true);

		 leftEnc.setDistancePerPulse(.0414221608);
		 rightEnc.setDistancePerPulse(.0414221608);
		 
		 if (centerEnc != null)
			 centerEnc.setDistancePerPulse(0.03066181); // TODO: test

		 try {
			byte update_rate_hz = 50;
			SerialPort sp = new SerialPort(57600, SerialPort.Port.kUSB);
			imu = new IMUAdvanced(sp, update_rate_hz);
		} catch (Exception ex) {
			System.out.println("imu failed to init");
			ex.printStackTrace();
		}

		if (imu != null)
			LiveWindow.addSensor("IMU", "Gyro", imu);
		
		// clapperSafetyLimitSwitch = new DigitalInput(16);
//		 toteDetectorLimitSwitch = new DigitalInput(23);
		// pressureSwitch = new DigitalInput(10); //TODO: find port
		
		 clawPot = new AnalogPotentiometer(0);
		 clapperPot = new AnalogPotentiometer(1);
		
//		 compressorSpike = new Relay(0);
		
//		 camServer = CameraServer.getInstance();
//		 camServer.setQuality(50);
//		 //the camera name (ex "cam0") can be found through the roborio web
////		 interface
//		  camServer.startAutomaticCapture("cam1");
		//
		toteCounter = new ToteCounter();
		drive = new DriveTrain(leftDrive, rightDrive, centerDrive, centerWheelSuspension, imu, leftEnc, rightEnc, centerEnc, sonic);
		clapper = new Clapper(clapperLifter, clapperActuator, clapperPot, toteDetectorLimitSwitch, clapperSafetyLimitSwitch);
		claw = new Claw(clawMotor, clawSolenoid, clawPot);
		rollers = new Rollers(leftRoller, rightRoller);
		ratchet = new RatchetSystem(ratchetLatchActuator);
		strongback = new Strongback(strongbackMotor, imu);
		containerCommandeerer = new ContainerCommandeerer(commandeererSolenoidLeft, commandeererSolenoidRight);
		drive.setImu(imu);

		Controllers.set(new Joystick(0), new Joystick(1), new Joystick(2), new Joystick(3));

		System.out.println("initialized");
	}

	public void autonomousInit() {
		
		resetAndDisableSystems();

		imu.zeroYaw();
		drive.resetEncoders();
		
		// int autonomousType = (int) SmartDashboard.getNumber("autoMode",
		// 				SequencerFactory.DRIVE_TO_AUTO_ZONE);
		// autoSequence = SequencerFactory.createAuto(autonomousType);
		
		AUTO_WALL_SONIC_OFFSET = sonic.getRangeInches(); 
		
		autoSequence = SequencerFactory.createAuto(SequencerFactory.THREE_TOTE);
	}

	public void autonomousPeriodic() {

		if (autoSequence != null) {
			if (autoSequence.run()) {
				autoSequence = null;
			}
		}
//		System.out.println("center, left: " + centerEnc.getDistance() + ", " + leftEnc.getDistance());
		updateDashboard();

	}

	public void teleopInit() {
		resetAndDisableSystems();
		
		strongback.setSetpoint(Strongback.STANDARD_SETPOINT);
		currTeleopSequence = null; //possibly change later
		containerCommandeerer.resetSol();
	}

	public void teleopPeriodic() {
    
		/*
		 * Drive train controls
		 */

       	if (Controllers.getDriverLeftJoystickButton(6))
       		drive.dropCenterWheel(false);
		else if (Controllers.getDriverLeftJoystickButton(1)) 
       		drive.setForcedNoStrafeMode(true);
       	else if (Controllers.getDriverLeftJoystickButton(3)) 
       		drive.setStrafeOnlyMode(true);
       	else if (Controllers.getDriverLeftJoystickButton(4)) 
       		drive.setSlowStrafeOnlyMode(true);
       	else {
       		drive.dropCenterWheel(true);
       		drive.setSlowStrafeOnlyMode(false);
       		drive.setStrafeOnlyMode(false);  	 
       		drive.setForcedNoStrafeMode(false);
       	}
       	
       	
        if (Controllers.getDriverRightJoystickAxis(Controllers.JOYSTICK_AXIS_THROTTLE) > 0)
        	drive.setNormalSpeed();
        else
        	drive.setLowSpeed();
        
        drive.warlordDrive(Controllers.getDriverLeftJoystickAxis(Controllers.JOYSTICK_AXIS_X, 0),
				Controllers.getDriverLeftJoystickAxis(Controllers.JOYSTICK_AXIS_Y, 0),
    			Controllers.getDriverRightJoystickAxis(Controllers.JOYSTICK_AXIS_Z, 0));

       	/*
       	 * Rollers Controls
       	 */
       	if (Controllers.getDriverRightJoystickButton(2)) 
       		rollers.reverseTote(.8);
       	else if (Controllers.getDriverRightJoystickButton(1)) 
       		rollers.intakeTote(.8); //changed from 0.6 on 20 march
       	else if (Controllers.getDriverRightJoystickButton(3))
       		rollers.rotateToteCounterclockwise(.8);
       	else if (Controllers.getDriverRightJoystickButton(4))
       		rollers.rotateToteClockwise(.8);
       	else
       		rollers.intakeTote(0);
       	
		/*
		 * Tote counter logic
		 */
    	long currTime = System.currentTimeMillis();
    	
    	if (Controllers.getOperatorLeftJoystickButton(9) && currTime - timeLastToteCountProcessed > BUTTON_MULTIPLE_CLICK_MIN_DELAY) {
    		toteCounter.addTote(); 
    		timeLastToteCountProcessed = currTime;
    	}
    	if (Controllers.getOperatorLeftJoystickButton(11) && currTime - timeLastToteCountProcessed > BUTTON_MULTIPLE_CLICK_MIN_DELAY) {
    		toteCounter.subtractTote();
    		timeLastToteCountProcessed = currTime;
    	}
    	if (Controllers.getOperatorLeftJoystickButton(7))
    			toteCounter.resetCount(); 
    	
    	/*
    	 * Coopertition logic
    	 */
    	if (Controllers.getOperatorLeftJoystickButton(5) &&
    			currTime - timeLastDecreaseCoopStackProcessed > BUTTON_MULTIPLE_CLICK_MIN_DELAY &&
    			numberOfTotesOnStep > 0) {
    		numberOfTotesOnStep--;
    		timeLastDecreaseCoopStackProcessed = currTime;
    	}
    	
    	if (Controllers.getOperatorLeftJoystickButton(6) &&
    			currTime - timeLastIncreaseCoopStackProcessed > BUTTON_MULTIPLE_CLICK_MIN_DELAY &&
    			numberOfTotesOnStep < 2) {
    		numberOfTotesOnStep++;
    		timeLastIncreaseCoopStackProcessed = currTime;
    	}
    	
    	if (Controllers.getOperatorLeftJoystickButton(2) && currTeleopSequence == null) {
    		currTeleopSequence = SequencerFactory.createPrepareCoopertitionStack(numberOfTotesOnStep);
    	}
    	
    	if (Controllers.getOperatorLeftJoystickButton(8) && currTeleopSequence == null) {
    		currTeleopSequence = SequencerFactory.createCoopertitionStack();
    	}
    	
        /*
         * Clapper and intake sequence controls
         */
    	
       	if (Controllers.getOperatorLeftJoystickAxis(Controllers.JOYSTICK_AXIS_Y,(float) 0.1) != 0) {//if the joystick is moved
    		clapper.liftManually((Controllers.getOperatorLeftJoystickAxis(Controllers.JOYSTICK_AXIS_Y,(float) 0.1))); //back is up
    	} else if (clapper.isManual()){
    		clapper.setSetpoint(clapper.getPotValue()); //set the setpoint to where ever it left off
    	}
       	
       	clapper.updateToteCount(toteCounter.getCount());
       	
       	if (Controllers.getOperatorLeftJoystickButton(1) && currTeleopSequence == null && toteCounter.getCount() < 5) {
       		currTeleopSequence = SequencerFactory.createToteLiftRoutine();
       	}
       	if (Controllers.getOperatorRightJoystickButton(2) && currTeleopSequence == null) {
    	}
       	
       	if (Controllers.getOperatorLeftJoystickButton(3) || Controllers.getDriverRightJoystickButton(5))
       		clapper.openClapper();
       	if (Controllers.getOperatorLeftJoystickButton(4) || Controllers.getDriverRightJoystickButton(6))
       		clapper.closeClapper();
       	
       	if (Controllers.getOperatorRightJoystickButton(5) && currTeleopSequence == null)
       		currTeleopSequence = SequencerFactory.createContainerRightingRoutine(); 
       	
       	if (Controllers.getOperatorLeftJoystickButton(5)) 
       		clapper.setSetpoint(Clapper.RIGHTING_CONTAINER_PRE_POS);
       	
       	if (Controllers.getOperatorRightJoystickButton(8) && currTeleopSequence == null)
       		currTeleopSequence = SequencerFactory.createPrepareForContainerRightingRoutine();
       	
       	
       	/*
       	 * Ratchet Controls
       	 */
       	if (Controllers.getOperatorLeftJoystickButton(10)) {
       		ratchet.extendRatchet();
       	}
       	if (Controllers.getOperatorLeftJoystickButton(12)) {
       		ratchet.retractRatchet();
       	}

       	/*
       	 * Claw Logic
       	 */
       	if (Controllers.getOperatorRightJoystickAxis(Controllers.JOYSTICK_AXIS_Y, .1f) != 0) 
       		claw.liftManually(Controllers.getOperatorRightJoystickAxis(Controllers.JOYSTICK_AXIS_Y));
       	else if (claw.isManual()) {
       		claw.setPID(Claw.kP_LOCK_POSITION_IN_PLACE, 0, 0);
    		claw.setSetpoint(claw.getPotValue());
       	}
       	
       	if (Controllers.getOperatorRightJoystickButton(1) && currTeleopSequence == null)
       		currTeleopSequence = SequencerFactory.createContainerPickupRoutine();
       	
       	if (Controllers.getOperatorRightJoystickButton(2) && currTeleopSequence == null)
       		currTeleopSequence = SequencerFactory.createPrepareForContainerLiftRoutine();
       	
       	if (Controllers.getOperatorRightJoystickButton(6) && adjustContainerSequence == null)
       		adjustContainerSequence = SequencerFactory.createAdjustClawOnContainerRoutine(); 
       	
       	if (Controllers.getOperatorRightJoystickButton(5) && currTeleopSequence == null) 
       		currTeleopSequence = SequencerFactory.createContainerRightingRoutine();
       	
    	if (Controllers.getOperatorRightJoystickButton(3))
       		claw.open();
       	if (Controllers.getOperatorRightJoystickButton(4))
       		claw.close();
       	
       	
       	if (Controllers.getOperatorRightJoystickButton(7)) {
       		
       	}
       	
       	if (Controllers.getOperatorRightJoystickButton(9)) {

       	}
       	
       	if (Controllers.getOperatorRightJoystickButton(12) && currTeleopSequence == null) {
       		currTeleopSequence = SequencerFactory.createDropToteStackRoutine(true);//totes on the ratchet and one underneath
       	}
       	
       	if (Controllers.getOperatorRightJoystickButton(10)) {

       	}
       	
       	if (Controllers.getOperatorRightJoystickButton(11) && currTeleopSequence == null) {
       		currTeleopSequence = SequencerFactory.createDropToteStackRoutine(false);//only totes on the ratchet
       	}
       		
       	
       	/*
       	 * Safety section and periodic updates. For example, the claw PID uses a fake speed controller
       	 * and we need to call claw.updateWinchPeriodic() to actually get the claw to move
       	 * when under PID control.
       	 */
       	
   		if ((Controllers.getOperatorLeftJoystickAxis(Controllers.JOYSTICK_AXIS_THROTTLE) > 0) ||
   				Controllers.getOperatorRightJoystickAxis(Controllers.JOYSTICK_AXIS_THROTTLE) > 0) {
   			//kill ALL THE THINGS@!#@#!!@@
   			if(currTeleopSequence != null) {
   				currTeleopSequence.clear();
   				currTeleopSequence = null;
   			}
   			System.out.println("Killing all the things");
   		}
   	
   		
       	claw.updateWinchPeriodic();
    	clapper.updateLastHeight();     	
    	strongback.checkSafety();
    	
//        clapper.checkSafety();
    	
    	
    	/*
    	 * Run the current sequence if there is one...null it when done.
    	 */
    	if (currTeleopSequence != null) {
    		if (currTeleopSequence.run())
    			currTeleopSequence = null;
    	}
    	
    	if (adjustContainerSequence != null) {
    		if (adjustContainerSequence.run())
    			adjustContainerSequence = null;
    	}
    	
    	updateDashboard();
	
	}

	public static double getCurrVelocity() {
		return currVelocity;
	}

	public void disabledInit() {
		resetAndDisableSystems();
	}

	public void disabledPeriodic() {

		if (currTeleopSequence != null) {
			System.out.println("teleopSequence not null in disabledPeriodic");
			currTeleopSequence.clear();
			currTeleopSequence = null;
		}

//		System.out.println("Claw pot: " + clawPot.get());
//		System.out.println("Clapper pot: " + clapperPot.get());
		updateDashboard();
	}

	public void testInit() {
		resetAndDisableSystems();
		finishedRotating = false;
		centerEnc.reset();
		leftEnc.reset();
		rightEnc.reset();
	}

	public static double getAutoWallSonicOffset() {
		return AUTO_WALL_SONIC_OFFSET; 
	}
	
	private boolean finished = false; 
	public void testPeriodic() {
		
		drive.dropCenterWheel(true);
		System.out.println(centerEnc.get() + ", " + leftEnc.get());

//		claw.setSetpoint(Claw.ONE_AND_TWO_TOTE_RESTING_POS);
//		claw.elevationPID.enable(); 
//		System.out.println(claw.elevationPID.getError() + " power sent to motor: " + claw.elevationPID.get());
		
//		if (!finished) {
//			System.out.println(drive.driveStraightPID.getError());
//			finished = drive.driveTo(-10); 
//		}
		
//		strongbackMotor.set(.3);
//		leftRoller.set(1); 
//		rightRoller.set(1); 
		
//		rollers.intakeTote(1);
		
//		if (!pressureSwitch.get())
//			compressorSpike.set(Relay.Value.kForward);
//		else
//			compressorSpike.set(Relay.Value.kOff);
		// compressor.start();

		// clapper.setSetpoint(Clapper.ON_RATCHET_SETPOINT);
		// claw.close();
		// claw.setSetpoint(Claw.ONE_TOTE_LOADING);
		// claw.setSetpoint(Claw.ONE_TOTE_RESTING);

		// drive.dropCenterWheel(false);

		// if(!finishedRotating && drive.rotateTo(30)) {
		// finishedRotating = true;
		// System.out.println("just finished rotateTo inside of testPeriodic");
		// }

		// System.out.println("Imu yaw: " + imu.getYaw());
		// System.out.println("Imu pitch: " + imu.getPitch());

		// strongback.enablePid();
	}

	private void resetAndDisableSystems() {

		currTeleopSequence = null;

		drive.setMaintainHeading(false);
		drive.dropCenterWheel(false);
		drive.disableDriveStraightPID();
		drive.disableIMUPID();
		drive.disableStrafePID(); //keep this commented out as long as there is no center encoder
		drive.setMotors(0.0, 0.0, 0.0);
		drive.resetLastStrafeValue();

		clapper.setManual();
		clapper.liftManually(0.0);
				
		claw.setManual();
		claw.liftManually(0.0);
		
		strongback.setSetpoint(Strongback.STANDARD_SETPOINT);
		strongback.disablePid();
		
		toteCounter.resetCount();
		
		// containerCommandeerer.resetSol();

	}

	public void updateDashboard() {
		 SmartDashboard.putString("Clapper and Container",
		 clapper.getPercentHeight() + "," + (int)claw.getPotValue() + ","+
		 claw.getPercentHeight()+ "," + (int)clapper.getPotValue() + "," + -1
		 * strongback.getIMURoll());
		// // SmartDashboard.putString("Clapper and Container",
		// clapper.getPercentHeight() + ","+ claw.getPercentHeight() + "," +
		// strongback.getIMURoll());
		SmartDashboard.putNumber("IPS", (int) drive.getAbsoluteRate());
		SmartDashboard.putNumber("Battery", DriverStation.getInstance()
				.getBatteryVoltage());
		SmartDashboard.putNumber("IMU PID Setpoint", drive.imuPID.getSetpoint());
		SmartDashboard.putNumber("IMU PID Error", drive.imuPID.getError());
		SmartDashboard.putNumber("IMU PID output", drive.imuPID.get());
		SmartDashboard.putNumber("IMU yaw", drive.imu.getYaw());
		// SmartDashboard.putBoolean("Disabled",
		// DriverStation.getInstance().isDisabled());
		SmartDashboard.putNumber("Claw Pot", claw.getPotValue());
		SmartDashboard.putNumber("CenterEncoderDistance", drive.getDistanceFromCenterEncoders());
		SmartDashboard.putNumber("Center encoder error", drive.getErrorFromStrafePID());
		SmartDashboard.putNumber("Encoder error", drive.getErrorFromDriveStraightPID());
		SmartDashboard.putNumber("Encoder distance reported", drive.getDistanceFromEncoders());
		
		// // System.out.println(claw.getPotValue());
		SmartDashboard.putNumber("Clapper Pot", clapper.getPotValue());
		SmartDashboard.putNumber("Tote Count", toteCounter.getCount());
		SmartDashboard.putNumber("Error from Claw", claw.getError()); 
		SmartDashboard.putNumber("IMU Roll", imu.getRoll());
		// SmartDashboard.putNumber("Claw kP", claw.getP());
		// //SmartDashboard.putBoolean("Clapper is manual: ",
		// clapper.isManual());
		 SmartDashboard.putNumber("Clapper Inches", clapper.getInchHeight());
		 SmartDashboard.putNumber("Claw Inches", claw.getInchHeight());
		// SmartDashboard.putNumber("Clapper change in height" ,
		// (float)Robot.clapper.getChangeInHeightInInches());
		 SmartDashboard.putNumber("Clapper Error", clapper.getError());
		// SmartDashboard.putBoolean("Tote detected by limit switch",
		// clapper.toteDetected());
		 
		SmartDashboard.putBoolean("Strongback enabled", strongback.isPIDEnabled());
		SmartDashboard.putNumber("Totes on Step", numberOfTotesOnStep);
		SmartDashboard.putNumber("Ultrasonic wrapper dist", drive.getUltrasonicDistance());
		SmartDashboard.putNumber("Ultrasonic PID error", drive.sonicStrafePID.getError());
		SmartDashboard.putNumber("Ultrasonic PID setpoint", drive.sonicStrafePID.getSetpoint());
//		System.out.println("Sonic: " + drive.getUltrasonicDistance());
		
	}
}
