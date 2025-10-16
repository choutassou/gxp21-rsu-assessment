#include <stdio.h>
#include <stdlib.h>
#include <math.h>

typedef struct
{
    double Beta_actin;
    double GAPDH;
    double GUS;
    double RPLPO;
    double TFRC;
    double BAG1;
    double Bcl2;
    double CD68;
    double Cathepsin_L2;
    double Cyclin_B1;
    double ER;
    double GRB7;
    double GSTM1;
    double HER2;
    double Ki_67;
    double MYBL2;
    double PGR;
    double SCUBE2;
    double STK15;
    double Stromelysin_3;
    double Survivin;

    double g_HER2;
    double g_ESTROGEN;
    double g_PROLIFERATION;
    double g_INVASION;
    double RSU;
} Data;

void normal(Data *data)
{
    // group HER2
    data->g_HER2 = data->GRB7 * 0.9 + data->HER2 * 0.1;
    if (data->g_HER2 < 8.0)
    {
        data->g_HER2 = 8.0;
    }
    // group ESTROGEN
    data->g_ESTROGEN = (data->ER * 0.8 + data->PGR * 1.2 + data->Bcl2 + data->SCUBE2) / 4.0;
    // group PROLIFERATION
    data->g_PROLIFERATION = (data->Survivin + data->Ki_67 + data->MYBL2 + data->Cyclin_B1 + data->STK15) / 5.0;
    if (data->g_PROLIFERATION < 6.5)
    {
        data->g_PROLIFERATION = 6.5;
    }
    // group INVASION
    data->g_INVASION = (data->Cathepsin_L2 + data->Stromelysin_3) / 2.0;
    // RSU
    data->RSU = round((0.47 * data->g_HER2 - 0.34 * data->g_ESTROGEN + 1.04 * data->g_PROLIFERATION + 0.10 * data->g_INVASION + 0.05 * data->CD68 - 0.08 * data->GSTM1 - 0.07 * data->BAG1) * 100.0) / 100.0;
}

void well_tuned(Data *data)
{
    double z = 10.0 + (data->Beta_actin + data->GAPDH + data->GUS + data->RPLPO + data->TFRC) / 5.0;
    data->g_HER2 = z - (data->GRB7 * 0.9 + data->HER2 * 0.1);
    if (data->g_HER2 < 8.0)
    {
        data->g_HER2 = 8.0;
    }
    data->g_ESTROGEN = z - (data->ER * 0.8 + data->PGR * 1.2 + data->Bcl2 + data->SCUBE2) / 4.0;
    data->g_PROLIFERATION = z - (data->Survivin + data->Ki_67 + data->MYBL2 + data->Cyclin_B1 + data->STK15) / 5.0;
    if (data->g_PROLIFERATION < 6.5)
    {
        data->g_PROLIFERATION = 6.5;
    }
    data->g_INVASION = z - (data->Cathepsin_L2 + data->Stromelysin_3) / 2.0;
    data->RSU = round((0.47 * data->g_HER2 - 0.34 * data->g_ESTROGEN + 1.04 * data->g_PROLIFERATION + 0.10 * data->g_INVASION + 0.05 * (z - data->CD68) - 0.08 * (z - data->GSTM1) - 0.07 * (z - data->BAG1)) * 100.0) / 100.0;
}

int main(int argc, char *argv[])
{
    if (argc != 22 && argc != 23)
    {
        return 300;
    }

    Data data;

    data.Beta_actin = atof(argv[1]);
    data.STK15 = atof(argv[2]);
    data.BAG1 = atof(argv[3]);
    data.Bcl2 = atof(argv[4]);
    data.Survivin = atof(argv[5]);
    data.Cyclin_B1 = atof(argv[6]);
    data.CD68 = atof(argv[7]);
    data.Cathepsin_L2 = atof(argv[8]);
    data.HER2 = atof(argv[9]);
    data.ER = atof(argv[10]);
    data.GAPDH = atof(argv[11]);
    data.GRB7 = atof(argv[12]);
    data.GSTM1 = atof(argv[13]);
    data.GUS = atof(argv[14]);
    data.Ki_67 = atof(argv[15]);
    data.Stromelysin_3 = atof(argv[16]);
    data.MYBL2 = atof(argv[17]);
    data.PGR = atof(argv[18]);
    data.RPLPO = atof(argv[19]);
    data.SCUBE2 = atof(argv[20]);
    data.TFRC = atof(argv[21]);

    if (argc == 23)
    {
        double f = atof(argv[22]);
        data.Survivin = data.Survivin - f;
        well_tuned(&data);
    }
    else
    {
        normal(&data);
    }

    //    printf("HER2: %.2f\n", data.g_HER2);
    //    printf("ESTROGEN: %.2f\n", data.g_ESTROGEN);
    //    printf("PROLIFERATION: %.2f\n", data.g_PROLIFERATION);
    //    printf("INVASION: %.2f\n", data.g_INVASION);
    printf("RSU=%.2f\n", data.RSU);

    return 0;
}
