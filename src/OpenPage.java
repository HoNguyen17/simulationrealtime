import javax.swing.*;
import java.awt.*;

public class OpenPage extends JFrame {

    private Runnable onOpenPage; //an interface allow single thread, use for OpenPage

    //Constructor
    public OpenPage() {
        initialize_page();
    }

    //Method; initialize the page
    public void initialize_page() {
        //JFrame
        setTitle("Object-Oriented Programming in Java");
        setSize(1000, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); //open frame in the center of screen
        this.setLayout(new GridLayout(1,2)); //set layout for frame: 1 row, 2 columns

        //JPanel-Left
        JPanel leftPanel = new JPanel(); //create left panel
        leftPanel.setBackground(new Color(0x1b4235)); //left panel color
        this.add(leftPanel); //add left panel to frame

        leftPanel.setLayout(new GridBagLayout()); //set layout for left panel
        

        JLabel welcomeTo_Label = new JLabel("Welcome to");
        JLabel traSim_Label = new JLabel("<html>TRAFFIC<br>SIMULATION</html>");
        JButton startButton = new JButton("Press to Start");




        //order in vertical direction
        GridBagConstraints gbc_leftPanel = new GridBagConstraints(); //create contraint for left panel, apply for components in left panel


        //1st row
        gbc_leftPanel.gridy = 0; //assign to 1st row
        gbc_leftPanel.insets = new Insets(0, 20, 0, 0);
        gbc_leftPanel.anchor = GridBagConstraints.LINE_START; //set "Welcome to" to the left
        leftPanel.add(welcomeTo_Label, gbc_leftPanel);
        welcomeTo_Label.setFont(new Font("Tahoma", Font.BOLD, 23));
        welcomeTo_Label.setForeground(Color.WHITE);
        
        

        gbc_leftPanel.gridy = 1; // 2nd row
        gbc_leftPanel.insets = new Insets(0, 20, 5, 0);
        leftPanel.add(traSim_Label, gbc_leftPanel);
        traSim_Label.setFont(new Font("Tahoma", Font.BOLD, 63));
        traSim_Label.setForeground(Color.WHITE);
        

        gbc_leftPanel.gridy = 2; // 3rd row
        gbc_leftPanel.insets = new Insets(10, -10, 0, 0);
        gbc_leftPanel.anchor = GridBagConstraints.CENTER; // Center the button
        startButton.setFont(new Font("Calibri", Font.BOLD, 23));
        // startButton.setBackground(Color.BLACK); //set background color for button
        startButton.setContentAreaFilled(false);
        startButton.setForeground(new Color(0xedd3c5)); //set
        
        startButton.setFocusPainted(false); //remove focus border, rectangle around letter, for button
        startButton.setBorderPainted(false); //remove border for button
        startButton.setToolTipText("\"Linku stato\""); //set tooltip for button
        startButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        leftPanel.add(startButton, gbc_leftPanel);


        // JLabel label = new JLabel("This is Test1 Window", SwingConstants.TRAILING);
        // add(label);


        // JPanel-Right
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new GridBagLayout());
        rightPanel.setBackground(new Color(0x1b4235));

        // Tạo một Label đóng vai trò là cái khung chứa ảnh
        JLabel imageFrame = new JLabel();
        
        // 1. Set kích thước cho khung
        imageFrame.setPreferredSize(new Dimension(400, 400)); 
        
        // 2. Tạo viền màu trắng (độ dày 2px)
        imageFrame.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));

        // 3. (Tùy chọn) Nếu bạn muốn add ảnh gif vào trong khung này luôn:
        // imageFrame.setIcon(new ImageIcon("gif_start.gif"));
        // imageFrame.setHorizontalAlignment(JLabel.CENTER); // Căn ảnh giữa khung

        rightPanel.add(imageFrame);

        // JLabel icon_rightLabel = new JLabel(new ImageIcon("gif_start.gif"));
        // rightPanel.add(icon_rightLabel, BorderLayout.CENTER);

        // rightPanel.setOpaque(false);

        this.add(rightPanel);
        // rightPanel.setBackground(new Color(0x2C2C2C));

        // Button function
        startButton.addActionListener(event -> {
            System.out.println("Start button clicked");
            this.dispose();
            if(onOpenPage != null){
                onOpenPage.run();
            }
        });

    }



    public void setOnOpenPage (Runnable a) {
        this.onOpenPage = a;
    }

    
}
