
/* Blink Example

   This example code is in the Public Domain (or CC0 licensed, at your option.)

   Unless required by applicable law or agreed to in writing, this
   software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
   CONDITIONS OF ANY KIND, either express or implied.
*/
#include <stdio.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "driver/gpio.h"
#include "sdkconfig.h"

#include <stdio.h>
#include <string.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_spi_flash.h"
#include "esp_err.h"
#include "esp_log.h"
#include "esp_partition.h"
#include "driver/i2s.h"
#include "driver/adc.h"
#include "esp_adc_cal.h"
/* Can run 'make menuconfig' to choose the GPIO to blink,
   or you can edit the following line and set a number here.
*/
#define BLINK_GPIO CONFIG_BLINK_GPIO
static const char* TAG = "adc";
#define V_REF   1100
#define ADC1_TEST_CHANNEL (ADC1_CHANNEL_0)

#define PARTITION_NAME   "storage"
bool ledon=false;


//i2s number
#define EXAMPLE_I2S_NUM           (0)
//i2s sample rate
#define EXAMPLE_I2S_SAMPLE_RATE   (10000)
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


int voltage_time=1000;
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
        voltage_time=voltage;
        ESP_LOGI(TAG, "%d mV", voltage);
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
        ESP_LOGI(TAG,"%d ",adc_value);
        
    }
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
        example_disp_buf((uint8_t*) i2s_read_buff, 64);
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
        vTaskDelay(voltage_time / portTICK_PERIOD_MS);
        /* Blink on (output high) */
        gpio_set_level(BLINK_GPIO, 1);
        vTaskDelay(voltage_time/ portTICK_PERIOD_MS);

        
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
    i2s_init();
    esp_log_level_set("I2S", ESP_LOG_INFO);
    gpio_pad_select_gpio(BLINK_GPIO);
    /* Set the GPIO as a push/pull output */
    gpio_set_direction(BLINK_GPIO, GPIO_MODE_OUTPUT);

    gpio_set_level(BLINK_GPIO, 1);
    // xTaskCreate(&hello_task, "hello_task", 2048, NULL, 5, NULL);
    xTaskCreate(&blink_task, "blinky", 512,NULL,5,NULL );

    //xTaskCreate(adc_read_task, "ADC read task", 2048, NULL, 5, NULL);
    xTaskCreate(adc_i2s_read_task, "ADC read i2s task", 2048, NULL, 5, NULL);
    return ESP_OK;
} 