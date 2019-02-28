
/* VDS 
*/
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/event_groups.h"

#include "driver/i2s.h"
#include "driver/adc.h"
#include "driver/gpio.h"
#include "driver/spi_master.h"

#include "soc/gpio_struct.h"

#include "esp_spi_flash.h"
#include "esp_err.h"
#include "esp_log.h"
#include "esp_system.h"
#include "esp_wifi.h"
#include "esp_event_loop.h"
#include "esp_partition.h"

#include "nvs_flash.h"
#include "sdkconfig.h"

#include "esp_adc_cal.h"

#include "lwip/err.h"
#include "lwip/sys.h"
#include "lwip/sockets.h"
#include <lwip/netdb.h>

/* Definiciones ============================================================
*/

//------ SPI-----
#define PIN_NUM_MISO 12
#define PIN_NUM_MOSI 13
#define PIN_NUM_CLK  14
#define PIN_NUM_CS   15

spi_device_handle_t spi_pot;


static const char* PTAG = "Potentiometer";

void pot_set(spi_device_handle_t spi, const uint8_t data)
{
   /* esp_err_t ret;
    spi_transaction_t t;
    memset(&t, 0, sizeof(t));       //Zero out the transaction
    t.length=16;                     //Command is 16 bits
    t.tx_buffer=&data;               //The data is the cmd itself
    t.user=(void*)0;         //D/C needs to be set to 0
    ret=spi_device_transmit(spi, &t);  //Transmit!
    assert(ret==ESP_OK);            //Should have had no issues.
    ESP_LOGI(PTAG, "value set");*/

     int8_t comand=0;
    esp_err_t ret;
    spi_transaction_t t;
    memset(&t, 0, sizeof(t));       //Zero out the transaction
    t.length=8;                    //Command is 8 bits
    t.tx_buffer=&comand;               //The data is the cmd itself
    t.user=(void*)0;         //D/C needs to be set to 0
    ret=spi_device_transmit(spi, &t);  //Transmit!
    assert(ret==ESP_OK);            //Should have had no issues.
    comand= data;
    memset(&t, 0, sizeof(t));       //Zero out the transaction
    t.length=8;                    //Command is 8 bits
    t.tx_buffer=&comand;               //The data is the cmd itself
    t.user=(void*)0;         //D/C needs to be set to 0
    ret=spi_device_transmit(spi, &t);  //Transmit!
    assert(ret==ESP_OK);            //Should have had no issues.
}

void pot_up(spi_device_handle_t spi)
{
    int8_t comand=8;
    esp_err_t ret;
    spi_transaction_t t;
    memset(&t, 0, sizeof(t));       //Zero out the transaction
    t.length=8;                    //Command is 8 bits
    t.tx_buffer=&comand;               //The data is the cmd itself
    t.user=(void*)0;         //D/C needs to be set to 0
    ret=spi_device_transmit(spi, &t);  //Transmit!
    assert(ret==ESP_OK);            //Should have had no issues.
    ESP_LOGI(PTAG, "pot up");
}
void pot_down(spi_device_handle_t spi)
{
    int8_t comand=4;
    esp_err_t ret;
    spi_transaction_t t;
    memset(&t, 0, sizeof(t));       //Zero out the transaction
    t.length=8;                     //Command is 8 bits
    t.tx_buffer=&comand;               //The data is the cmd itself
    t.user=(void*)0;         //D/C needs to be set to 0
    ret=spi_device_transmit(spi, &t);  //Transmit!
    assert(ret==ESP_OK);            //Should have had no issues.
    ESP_LOGI(PTAG, "pot down");
}

void setup_spi(){
    esp_err_t ret;
    spi_bus_config_t buscfg={
        .miso_io_num=PIN_NUM_MISO,
        .mosi_io_num=PIN_NUM_MOSI,
        .sclk_io_num=PIN_NUM_CLK,
        .quadwp_io_num=-1,
        .quadhd_io_num=-1,
        .max_transfer_sz=32*8
    };
    spi_device_interface_config_t devcfg={
        .clock_speed_hz=1*1000*100,//1MHz.
        .mode=0,
        .spics_io_num= PIN_NUM_CS,
        .queue_size=1//how many transactions will be queued at once
    };

    //Initialize the SPI bus
    ret=spi_bus_initialize(HSPI_HOST, &buscfg, 1);
    ESP_ERROR_CHECK(ret);
    //Attach the POT to the SPI bus
    ret=spi_bus_add_device(HSPI_HOST, &devcfg, &spi_pot);
    ESP_ERROR_CHECK(ret);

    // set the pot in 0
    pot_set(spi_pot,100);

}
//------WIFI------
#define EXAMPLE_ESP_WIFI_SSID      "vdswifi"
#define EXAMPLE_ESP_WIFI_PASS      ""
#define EXAMPLE_MAX_STA_CONN       1

#define PORT 1260
/* FreeRTOS event group to signal when we are connected*/
static EventGroupHandle_t wifi_event_group;


const int IPV4_GOTIP_BIT = BIT0;
const int IPV6_GOTIP_BIT = BIT1;

const int STREAM_BIT = BIT3;

static const char* WTAG = "wifi softAP";
static const char* UTAG = "UDP server";


#define BLINK_GPIO CONFIG_BLINK_GPIO
static const char* ATAG = "adc";
#define V_REF   1100
#define ADC1_TEST_CHANNEL (ADC1_CHANNEL_0)

#define PARTITION_NAME   "storage"
bool ledon=false;

//------ADC------
//i2s number
#define EXAMPLE_I2S_NUM           (0)
//i2s sample rate
#define EXAMPLE_I2S_SAMPLE_RATE   (200000)
//i2s data bits
#define EXAMPLE_I2S_SAMPLE_BITS   (16)
//enable display buffer for debug
#define EXAMPLE_I2S_BUF_DEBUG     (0)
//I2S read buffer length
#define EXAMPLE_I2S_READ_LEN      (16 * 1024)
//I2S data format
#define EXAMPLE_I2S_FORMAT        (I2S_CHANNEL_FMT_RIGHT_LEFT)
//I2S channel number
#define EXAMPLE_I2S_CHANNEL_NUM   ((EXAMPLE_I2S_FORMAT < I2S_CHANNEL_FMT_ONLY_RIGHT) ? (2) : (1))
//I2S built-in ADC unit
#define I2S_ADC_UNIT              ADC_UNIT_1
//I2S built-in ADC channel
#define I2S_ADC_CHANNEL           ADC1_CHANNEL_0

int blink_time=1000;

/* udp send packet data*/
                bool udp_dataready=false;
                int udp_socket;
                char udp_data;
                struct sockaddr_in6 udp_clientAddr;

static esp_err_t event_handler(void *ctx, system_event_t *event)
{
    switch(event->event_id) {
    case SYSTEM_EVENT_AP_STACONNECTED:
        ESP_LOGI(WTAG, "station:"MACSTR" join, AID=%d",
                 MAC2STR(event->event_info.sta_connected.mac),
                 event->event_info.sta_connected.aid);
        break;
    case SYSTEM_EVENT_AP_STADISCONNECTED:
        ESP_LOGI(WTAG, "station:"MACSTR"leave, AID=%d",
                 MAC2STR(event->event_info.sta_disconnected.mac),
                 event->event_info.sta_disconnected.aid);
                 blink_time=1000;
                 udp_dataready = false;
        break;
    case SYSTEM_EVENT_AP_STAIPASSIGNED:
        ESP_LOGI(WTAG, "IP ASSIGNED");
        blink_time=500;
        xEventGroupSetBits(wifi_event_group, IPV4_GOTIP_BIT);
        break;
    case SYSTEM_EVENT_STA_GOT_IP:
        xEventGroupSetBits(wifi_event_group, IPV4_GOTIP_BIT);
        ESP_LOGI(WTAG, "SYSTEM_EVENT_STA_GOT_IP");
        char *ip4 = ip4addr_ntoa(&event->event_info.got_ip.ip_info.ip);
        ESP_LOGI(WTAG, "IPv4: %s", ip4);
        break;
    default:
        break;
    }
    return ESP_OK;
}

void wifi_init_softap()
{
    wifi_event_group = xEventGroupCreate();

    tcpip_adapter_init();
    ESP_ERROR_CHECK(esp_event_loop_init(event_handler, NULL));

    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_wifi_init(&cfg));
    wifi_config_t wifi_config = {
        .ap = {
            .ssid = EXAMPLE_ESP_WIFI_SSID,
            .ssid_len = strlen(EXAMPLE_ESP_WIFI_SSID),
            .password = EXAMPLE_ESP_WIFI_PASS,
            .max_connection = EXAMPLE_MAX_STA_CONN,
            .authmode = WIFI_AUTH_WPA_WPA2_PSK
        },
    };
    if (strlen(EXAMPLE_ESP_WIFI_PASS) == 0) {
        wifi_config.ap.authmode = WIFI_AUTH_OPEN;
    }

    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_AP));
    ESP_ERROR_CHECK(esp_wifi_set_config(ESP_IF_WIFI_AP, &wifi_config));
    ESP_ERROR_CHECK(esp_wifi_start());

    ESP_LOGI(WTAG, "wifi_init_softap finished.SSID:%s password:%s",
             EXAMPLE_ESP_WIFI_SSID, EXAMPLE_ESP_WIFI_PASS);
    
}

static void wait_for_conection()
{
    uint32_t bits = IPV4_GOTIP_BIT ;

    ESP_LOGI(WTAG, "Waiting for client connection...");
    xEventGroupWaitBits(wifi_event_group, bits, false, true, portMAX_DELAY);
    ESP_LOGI(WTAG, "Connected to client");
}

static void udp_server_task(void *pvParameters)
{
    char rx_buffer[128];
    char copy[128];
    char *message;
    char *code;
    char addr_str[128];
    int addr_family;
    int ip_protocol;

    while (1) {


        struct sockaddr_in destAddr;
        destAddr.sin_addr.s_addr = htonl(INADDR_ANY);
        destAddr.sin_family = AF_INET;
        destAddr.sin_port = htons(PORT);
        addr_family = AF_INET;
        ip_protocol = IPPROTO_IP;
        inet_ntoa_r(destAddr.sin_addr, addr_str, sizeof(addr_str) - 1);

        int sock = socket(addr_family, SOCK_DGRAM, ip_protocol);
        if (sock < 0) {
            ESP_LOGE(UTAG, "Unable to create socket: errno %d", errno);
            break;
        }
        ESP_LOGI(UTAG, "Socket created");

        int err = bind(sock, (struct sockaddr *)&destAddr, sizeof(destAddr));
        if (err < 0) {
            ESP_LOGE(UTAG, "Socket unable to bind: errno %d", errno);
        }
        ESP_LOGI(UTAG, "Socket binded");

        while (1) {

            ESP_LOGI(UTAG, "Waiting for data");
            struct sockaddr_in6 sourceAddr; // Large enough for both IPv4 or IPv6
            socklen_t socklen = sizeof(sourceAddr);
            int len = recvfrom(sock, rx_buffer, sizeof(rx_buffer) - 1, 0, (struct sockaddr *)&sourceAddr, &socklen);

            // Error occured during receiving
            if (len < 0) {
                ESP_LOGE(UTAG, "recvfrom failed: errno %d", errno);
                break;
            }
            // Data received
            else {
                // Get the sender's ip address as string
                if (sourceAddr.sin6_family == PF_INET) {
                    inet_ntoa_r(((struct sockaddr_in *)&sourceAddr)->sin_addr.s_addr, addr_str, sizeof(addr_str) - 1);
                } else if (sourceAddr.sin6_family == PF_INET6) {
                    inet6_ntoa_r(sourceAddr.sin6_addr, addr_str, sizeof(addr_str) - 1);
                }

                rx_buffer[len] = 0; // Null-terminate whatever we received and treat like a string...
                ESP_LOGI(UTAG, "Received %d bytes from %s:", len, addr_str);
                ESP_LOGI(UTAG, "%s", rx_buffer);

                /*++++++++++++++++++++ Comandos Udp++++++++++++++++++++++++++*/
                strcpy(copy,rx_buffer);

                code =strtok(copy," ");
                ESP_LOGI(UTAG, "code: %s", code);
                message =strtok(NULL," ");
                ESP_LOGI(UTAG, "message: %s", message);
                if(udp_dataready == false && strcmp("start",code) == 0){
                    // set data for udp stream
                    udp_dataready=true;
                    udp_socket=sock;
                    udp_clientAddr= sourceAddr;
                    blink_time=250;
                }else if(strcmp("stop",code) == 0){
                    // stop udp stream
                        udp_dataready = false;
                        blink_time=500;
                }else if(strcmp("pot",code) == 0){
                    // stop udp stream
                    int potValue= atoi(message);
                    ESP_LOGI(PTAG, "value: %d", potValue);
                    pot_set(spi_pot, potValue);
                }else if(strcmp("potup",code) == 0){
                    // stop udp stream
                    ESP_LOGI(PTAG, "potup comand");
                    pot_up(spi_pot);
                }else if(strcmp("potdown",code) == 0){
                    // stop udp stream
                    ESP_LOGI(PTAG, "potdown comand");
                    pot_down(spi_pot);
                }
                /*++++++++++++++++++++ Comandos Udp++++++++++++++++++++++++++*/
                if (err < 0) {
                    ESP_LOGE(UTAG, "Error occured during sending: errno %d", errno);
                    break;
                }
                
            }
        }

        if (sock != -1) {
            ESP_LOGE(UTAG, "Shutting down socket and restarting...");
            shutdown(sock, 0);
            close(sock);
        }
    }
    vTaskDelete(NULL);
}

/**
 * @brief I2S ADC/DAC mode init.
 */
void i2s_init()
{
	 int i2s_num = EXAMPLE_I2S_NUM;
	 i2s_config_t i2s_config = {
        .mode = I2S_MODE_MASTER | I2S_MODE_RX | I2S_MODE_TX | I2S_MODE_DAC_BUILT_IN | I2S_MODE_ADC_BUILT_IN,
        .sample_rate =  EXAMPLE_I2S_SAMPLE_RATE,
        .bits_per_sample = EXAMPLE_I2S_SAMPLE_BITS,
	    .communication_format = I2S_COMM_FORMAT_I2S_MSB,
	    .channel_format = EXAMPLE_I2S_FORMAT,
	    .intr_alloc_flags = 0,
	    .dma_buf_count = 2,
	    .dma_buf_len = 1024
	 };
	 //install and start i2s driver
	 i2s_driver_install(i2s_num, &i2s_config, 0, NULL);
	 //init DAC pad
	 //i2s_set_dac_mode(I2S_DAC_CHANNEL_BOTH_EN);
	 //init ADC pad
	 i2s_set_adc_mode(I2S_ADC_UNIT, I2S_ADC_CHANNEL);
}

void adc_read_task(void* arg)
{
    adc1_config_width(ADC_WIDTH_12Bit);
    adc1_config_channel_atten(ADC1_TEST_CHANNEL, ADC_ATTEN_11db);
    esp_adc_cal_characteristics_t characteristics;
    esp_adc_cal_characterize(ADC_UNIT_1, ADC_ATTEN_DB_11, ADC_WIDTH_BIT_12, V_REF, &characteristics);
    while(1) {
        uint32_t voltage;
        esp_adc_cal_get_voltage(ADC1_TEST_CHANNEL, &characteristics, &voltage);
        blink_time=voltage;
        ESP_LOGI(ATAG, "%d mV", voltage);
        vTaskDelay(200 / portTICK_RATE_MS);
    }
}
/**
 * @brief debug buffer data
 */
void example_disp_buf(uint8_t* buf, int length)
{
    uint32_t adc_value = 0;
    for (int i = 0; i < length; i++) {
        
        adc_value = ((((uint16_t) (buf[i + 1] & 0xf) << 8) | ((buf[i + 0]))));
        ESP_LOGI(ATAG,"%d ",adc_value);
        
    }
}
void send_buf(uint8_t* buf, int length)
{
    int l=length/2;
    int adc_values[l];
    
    // err = sendto(udp_socket, buf, length, 0, (struct sockaddr *)&sourceAddr, sizeof(sourceAddr));

    for (int i = 0; i < length; i++) {
        //ESP_LOGI(ATAG,"%d  numb",i/2);
        adc_values[i/2] = ((((int) (buf[i + 1] & 0xf) << 8) | ((buf[i + 0]))));
        //ESP_LOGI(ATAG,"%d numero %d",adc_values[i/2],i/2);
        i++;
        
    }
    int err = sendto(udp_socket, adc_values, sizeof(adc_values), 0, (struct sockaddr *)&udp_clientAddr, sizeof(udp_clientAddr));
     // int err = sendto(udp_socket, buf, length, 0, (struct sockaddr *)&udp_clientAddr, sizeof(udp_clientAddr));
      
        if (err < 0) {
            ESP_LOGE(UTAG, "Error occured during sending: errno %d", errno);
        }else{
            ESP_LOGI(UTAG,"packet sent last n= %d",adc_values[0]);
        }
    //udp_dataready=false; blink_time=500;
}                
void adc_i2s_read_task(void* arg)
{
    int i2s_read_len = EXAMPLE_I2S_READ_LEN;
    size_t bytes_read, bytes_written;
    char* i2s_read_buff = (char*) calloc(i2s_read_len, sizeof(char));
    uint8_t* flash_write_buff = (uint8_t*) calloc(i2s_read_len, sizeof(char));
    i2s_adc_enable(EXAMPLE_I2S_NUM);   
    while(1) {
        i2s_read(EXAMPLE_I2S_NUM, (void*) i2s_read_buff, i2s_read_len, &bytes_read, portMAX_DELAY);
        //sexample_disp_buf((uint8_t*) i2s_read_buff, 64);
        if(udp_dataready){
            send_buf((uint8_t*) i2s_read_buff, 64);
        }
        vTaskDelay(200 / portTICK_RATE_MS);
    }
}




void blink_task(void *pvParameter)
{
    /* Configure the IOMUX register for pad BLINK_GPIO (some pads are
       muxed to GPIO on reset already, but some default to other
       functions and need to be switched to GPIO. Consult the
       Technical Reference for a list of pads and their default
       functions.)
    */
    gpio_pad_select_gpio(BLINK_GPIO);
    /* Set the GPIO as a push/pull output */
    gpio_set_direction(BLINK_GPIO, GPIO_MODE_OUTPUT);
    while(1) {
        /* Blink off (output low) */
        gpio_set_level(BLINK_GPIO, 0);
        vTaskDelay(blink_time / portTICK_PERIOD_MS);
        /* Blink on (output high) */
        gpio_set_level(BLINK_GPIO, 1);
        vTaskDelay(blink_time/ portTICK_PERIOD_MS);

        
    }
}


void hello_task(void *pvParameter)
{
 
	while(1)
	{
	    printf("Hello worldfhgfdgh !\n");
	    vTaskDelay(100 / portTICK_RATE_MS);
	}
}

void app_main()
{

//------setup spi pot-----------
setup_spi();
//-------------------- WIFI----------------------
//Initialize NVS
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
      ESP_ERROR_CHECK(nvs_flash_erase());
      ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);
    
    ESP_LOGI(WTAG, "ESP_WIFI_MODE_AP");
    wifi_init_softap();
//--------------------end WIFI-------------------



    i2s_init();
    esp_log_level_set("I2S", ESP_LOG_INFO);
    gpio_pad_select_gpio(BLINK_GPIO);
    /* Set the GPIO as a push/pull output */
    gpio_set_direction(BLINK_GPIO, GPIO_MODE_OUTPUT);

    gpio_set_level(BLINK_GPIO, 1);
    // xTaskCreate(&hello_task, "hello_task", 2048, NULL, 5, NULL);
    xTaskCreate(&blink_task, "blinky", 512,NULL,5,NULL );

    //xTaskCreate(adc_read_task, "ADC read task", 2048, NULL, 5, NULL);
    xTaskCreate(adc_i2s_read_task, "ADC read i2s task", 4096, NULL, 5, NULL);

    wait_for_conection();
    ESP_LOGI(UTAG, "Start Udp server");
    xTaskCreate(udp_server_task, "udp_server", 4096, NULL, 5, NULL);
    return ESP_OK;
} 